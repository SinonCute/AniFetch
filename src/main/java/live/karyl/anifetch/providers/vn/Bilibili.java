package live.karyl.anifetch.providers.vn;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import live.karyl.anifetch.models.*;
import live.karyl.anifetch.providers.AnimeProvider;
import live.karyl.anifetch.types.AudioType;
import live.karyl.anifetch.types.LanguageType;
import live.karyl.anifetch.types.SubtitleType;
import live.karyl.anifetch.types.VideoType;
import live.karyl.anifetch.utils.SearchRequest;
import live.karyl.anifetch.utils.Utils;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Bilibili extends AnimeProvider {

	private static final String WATCH_URL = "https://www.bilibili.tv/en/play/";
	private static final String MEDIA_URL = "https://www.bilibili.tv/en/media/";
	private static final String API_URL = "https://api.bilibili.tv/intl/gateway/web/";

	public Bilibili() {
		super("Bilibili", "https://www.bilibili.tv/");
	}

	@Override
	public AnimeParser search(AnilistInfo anilistInfo) {
		AnimeParser animeParser = null;
		String redisId = siteName + "$" + anilistInfo.getId();
		Map<String, String> titles = new HashMap<>();
		titles.put("english", anilistInfo.getTitle().english);
		titles.put("romaji", anilistInfo.getTitle().romaji);

		if (redis.exists(redisId, REDIS_SEARCH)) {
			animeParser = new Gson().fromJson(redis.get(redisId, REDIS_SEARCH), AnimeParser.class);
			if (animeParser != null) return animeParser;
		}

		if (postgreSQL.checkAnimeFetchExists(anilistInfo.getId(), siteName)) {
			var id = postgreSQL.getAnimeFetch(anilistInfo.getId(), siteName);
			var episodes = extractEpisodeIds(connect(WATCH_URL + id, siteName));
			animeParser = new AnimeParser(anilistInfo.getId(), id, siteName);
			animeParser.setEpisodes(episodes);
			redis.set(redisId, animeParser.toJson(), REDIS_SEARCH);
			return animeParser;
		}

		for (var entry : titles.entrySet()) {
			var title = entry.getValue();
			if (animeParser != null) continue;
			var searchResults = SearchRequest.bilibili(title);
			if (searchResults == null) continue;
			for (var searchResult : searchResults) {
				var mainPage = connect(WATCH_URL + searchResult, siteName);
				if (compareResult(anilistInfo, mainPage, entry.getKey())) {
					var episodes = extractEpisodeIds(mainPage);
					animeParser = new AnimeParser(anilistInfo.getId(), searchResult, siteName);
					animeParser.setEpisodes(episodes);
					redis.set(redisId, animeParser.toJson(), REDIS_SEARCH);
					postgreSQL.addAnimeFetch(animeParser);
					break;
				}
			}
		}
		return animeParser;
	}

	@Override
	public AnimeSource getLink(String value) {
		try {
			AnimeSource animeSource = new AnimeSource(siteName);
			String redisId = siteName + "$" + value;

			if (redis.exists(redisId, REDIS_SOURCE)) {
				animeSource = new Gson().fromJson(redis.get(redisId, REDIS_SOURCE), AnimeSource.class);
				if (animeSource != null) return animeSource;
			}

			var mediaId = value.split("\\$")[0];
			var episodeId = value.split("\\$")[1];

			RequestBody requestResource = new FormBody.Builder()
					.addEncoded("url", API_URL + "playurl?ep_id=" + episodeId + "&device=wap&platform=web&qn=64&tf=0&type=0")
					.addEncoded("method", "GET")
					.addEncoded("header-user-agent", USER_AGENT)
					.add("header-Cookie", config.getBilibiliCookie())
					.build();

			RequestBody requestSubtile = new FormBody.Builder()
					.addEncoded("url", API_URL + "v2/subtitle?episode_id=" + episodeId + "&s_locale=en_US")
					.addEncoded("method", "GET")
					.addEncoded("header-user-agent", USER_AGENT)
					.add("header-Cookie", config.getBilibiliCookie())
					.build();

			Request request = new Request.Builder()
					.url(PROXY_VN)
					.post(requestResource)
					.build();

			Request subtileRequest = new Request.Builder()
					.url(PROXY_VN)
					.post(requestSubtile)
					.build();

			var response = connection.callWithoutRateLimit(request);
			var subtitleResponse = connection.callWithoutRateLimit(subtileRequest);
			var subtitleJson = new Gson().fromJson(subtitleResponse.body().string(), JsonObject.class);
			var resourceJson = new Gson().fromJson(response.body().string(), JsonObject.class);
			var playUrl = resourceJson.get("data").getAsJsonObject().getAsJsonObject("playurl");
			var subtitleArray = subtitleJson.get("data").getAsJsonObject().getAsJsonArray("video_subtitle");
			var audioArray = playUrl.getAsJsonArray("audio_resource");
			var videoArray = playUrl.getAsJsonArray("video");

			List<VideoResource> videoResources = new ArrayList<>();
			List<AudioResource> audioResources = new ArrayList<>();
			List<Subtitle> subtitleResources = new ArrayList<>();

			videoArray.forEach(video -> {
				var videoObject = video.getAsJsonObject().getAsJsonObject("video_resource");
				var videoUrl = videoObject.get("url").getAsString();
				var videoUrlBackup = videoObject.get("backup_url").getAsJsonArray().get(0).getAsString();
				var videoQuality = video.getAsJsonObject().getAsJsonObject("stream_info").get("desc_words").getAsString();
				var videoType = VideoType.DASH;
				var videoResource = new VideoResource(videoUrl, videoQuality, "bilibili", videoType);
				videoResource.setBackupUrl(videoUrlBackup);
				videoResource.setUseHeader(true);
				videoResources.add(videoResource);
			});

			audioArray.forEach(audio -> {
				var audioObject = audio.getAsJsonObject();
				var audioUrl = audioObject.get("url").getAsString();
				var audioUrlBackup = audioObject.get("backup_url").getAsJsonArray().get(0).getAsString();
				var audioQuality = audioObject.get("quality").getAsString();
				var audioResource = new AudioResource(audioUrl, audioQuality);
				audioResource.setBackupUrl(audioUrlBackup);
				audioResources.add(audioResource);
			});

			subtitleArray.forEach(subtitle -> {
				var subtitleObject = subtitle.getAsJsonObject();
				var langKey = subtitleObject.get("lang_key").getAsString();
				if (!(langKey.equals("en") || langKey.equals("vi"))) return;
				var subtitleLanguage = LanguageType.fromString(langKey);

				String ass = null;
				String srt = null;
				if (!subtitleObject.get("ass").isJsonNull()) {
					ass = subtitleObject.getAsJsonObject("ass").get("url").getAsString();
				}
				if (!subtitleObject.get("srt").isJsonNull()) {
					System.out.println(subtitleObject.getAsJsonObject("srt").get("url").getAsString());
					srt = subtitleObject.getAsJsonObject("srt").get("url").getAsString();
				}
				var subtitles = new Subtitle(subtitleLanguage, ass, srt);
				subtitleResources.add(subtitles);
			});

			animeSource.setVideoResources(videoResources);
			animeSource.setAudioResources(audioResources);
			animeSource.setSubtitles(subtitleResources);
			animeSource.setAudioType(AudioType.SOFT);
			animeSource.setSubtitleType(SubtitleType.SOFT);

			animeSource.addHeader("referer", new String[]{"https://www.bilibili.tv/en/play/" + mediaId});

			return animeSource;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private List<AnimeEpisode> extractEpisodeIds(Document mainPage) {
		List<AnimeEpisode> episodes = new ArrayList<>();
		var episodeElements = mainPage.select(".ep-list > .ep-item > div > a");

		for (var episodeElement : episodeElements) {
			var episodeId = episodeElement.attr("href");
			var episodeTitle = episodeElement.text();
			var episodeNumber = extractNumberFromString(episodeTitle);
			var pattern = Pattern.compile("/play/([0-9]+)/([0-9]+)\\?.*");
			var matcher = pattern.matcher(episodeId);
			if (!matcher.find()) continue;
			episodes.add(new AnimeEpisode(episodeTitle, episodeNumber, matcher.group(1) + "$" + matcher.group(2)));
		}

		return episodes;
	}

	private boolean compareResult(AnilistInfo anilistInfo, Document mainPage, String type) {
		if (mainPage == null) return false;
		var year = Integer.parseInt(mainPage.select(".bstar-meta__create-time").text().split(",")[1].trim());
		var originalTitle = mainPage.select(".bstar-meta__origin-name-content").text();
		var title = mainPage.select(".bstar-meta__ogv-title").text();
		var episode = mainPage.select(".ep-list > .ep-item").size();

		if (type.equals("english")) {
			return year == anilistInfo.getReleaseDate()
					&& Utils.checkNumberEqual(episode, anilistInfo.getCurrentEpisode())
					&& Utils.matchedRate(title, anilistInfo.getTitle().english) > 0.5;
		} else if (anilistInfo.getTitle().nativeTitle != null) {
			return year == anilistInfo.getReleaseDate()
					&& Utils.checkNumberEqual(episode, anilistInfo.getCurrentEpisode())
					&& Utils.matchedRate(originalTitle, anilistInfo.getTitle().nativeTitle) > 0.6;
		}
		return year == anilistInfo.getReleaseDate()
				&& Utils.checkNumberEqual(episode, anilistInfo.getCurrentEpisode())
				&& Utils.matchedRate(title, anilistInfo.getTitle().romaji) > 0.5;
	}
}
