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
import org.jsoup.nodes.Element;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class Bilibili extends AnimeProvider {

	private static final String WATCH_URL = "https://www.bilibili.tv/en/play/";
	private static final String MEDIA_URL = "https://www.bilibili.tv/en/media/";
	private static final String API_URL = "https://api.bilibili.tv/intl/gateway/web/";

	public Bilibili() {
		super("Bilibili", "BL", "https://www.bilibili.tv/");
	}

	@Override
	public AnimeParser search(AnilistInfo anilistInfo) {
		AnimeParser animeParser = null;
		String redisId = siteName + "$" + anilistInfo.getId();
		Map<String, String> titles = new HashMap<>();
		titles.put("english", anilistInfo.getTitle().english);
		titles.put("romaji", anilistInfo.getTitle().romaji);

		if (redis.exists(redisId, REDIS_NON_EXIST)) {
			return null;
		}

		if (redis.exists(redisId, REDIS_SEARCH)) {
			animeParser = new Gson().fromJson(redis.get(redisId, REDIS_SEARCH), AnimeParser.class);
			if (animeParser != null) return animeParser;
		}

		if (postgreSQL.checkAnimeFetchExists(anilistInfo.getId(), siteName)) {
			var id = postgreSQL.getAnimeFetch(anilistInfo.getId(), siteName);
			var episodes = extractEpisodeIds(connect(WATCH_URL + id, siteName));
			animeParser = new AnimeParser(anilistInfo.getId(), id, siteId, siteName);
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
					animeParser = new AnimeParser(anilistInfo.getId(), searchResult, siteId, siteName);
					animeParser.setEpisodes(episodes);
					redis.set(redisId, animeParser.toJson(), REDIS_SEARCH);
					postgreSQL.addAnimeFetch(animeParser);
					break;
				}
			}
		}
		if (animeParser == null) {
			redis.set(redisId, "null", REDIS_NON_EXIST);
		}
		return animeParser;
	}

	@Override
	public AnimeSource getLink(String value, boolean ignoreCache) {
		try {
			AnimeSource animeSource = new AnimeSource(siteName);
			String redisId = siteName + "$" + value;

			if (redis.exists(redisId, REDIS_SOURCE) && !ignoreCache) {
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
					.url(config.getProxyVNBackup())
					.post(requestResource)
					.build();

			Request subtileRequest = new Request.Builder()
					.url(config.getProxyVNBackup())
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
				var duration = videoObject.get("duration").getAsInt();
				var bandwidth = videoObject.get("bandwidth").getAsInt();
				var videoUrl = videoObject.get("url").getAsString();
				var videoUrlBackup = videoObject.get("backup_url").getAsJsonArray().get(0).getAsString();
				var codecs = videoObject.get("codecs").getAsString();
				var sar = videoObject.get("sar").getAsString();
				var frameRate = videoObject.get("frame_rate").getAsString();
				var range = videoObject.getAsJsonObject("segment_base").get("range").getAsString();
				var indexRange = videoObject.getAsJsonObject("segment_base").get("index_range").getAsString();
				var width = videoObject.get("width").getAsInt();
				var height = videoObject.get("height").getAsInt();
				var mimeTypes = videoObject.get("mime_type").getAsString();
				var videoQuality = video.getAsJsonObject().getAsJsonObject("stream_info").get("desc_words").getAsString();
				var audioQuality = video.getAsJsonObject().get("audio_quality").getAsString();
				var videoType = VideoType.DASH;
				var videoResource = new VideoResource(videoUrl, videoQuality, "bilibili", videoType);
				videoResource.setDuration(duration);
				videoResource.setBandwidth(bandwidth);
				videoResource.setMimeType(mimeTypes);
				videoResource.setSar(sar);
				videoResource.setFrameRate(frameRate);
				videoResource.setRange(range);
				videoResource.setIndexRange(indexRange);
				videoResource.setWidth(width);
				videoResource.setHeight(height);
				videoResource.setCodecs(codecs);
				videoResource.setBackupUrl(videoUrlBackup);
				videoResource.setAudioQuality(audioQuality);
				videoResource.setUseHeader(true);
				videoResources.add(videoResource);
			});

			audioArray.forEach(audio -> {
				var audioObject = audio.getAsJsonObject();
				var bandwidth = audioObject.get("bandwidth").getAsInt();
				var codecs = audioObject.get("codecs").getAsString();
				var range = audioObject.getAsJsonObject("segment_base").get("range").getAsString();
				var indexRange = audioObject.getAsJsonObject("segment_base").get("index_range").getAsString();
				var mimeType = audioObject.get("mime_type").getAsString();
				var audioUrl = audioObject.get("url").getAsString();
				var audioUrlBackup = audioObject.get("backup_url").getAsJsonArray().get(0).getAsString();
				var audioQuality = audioObject.get("quality").getAsString();
				var audioResource = new AudioResource(audioUrl, audioQuality);
				audioResource.setBandwidth(bandwidth);
				audioResource.setCodecs(codecs);
				audioResource.setRange(range);
				audioResource.setIndexRange(indexRange);
				audioResource.setMimeType(mimeType);
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

			animeSource.addHeader("referer", "https://www.bilibili.tv/en/play/" + mediaId);

			redis.set(redisId, animeSource.toJson(), REDIS_SOURCE);
			checkVideoStream(videoResources, value);
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
		if (mainPage == null) {
			Logger.error("Bilibili | Main page is null");
			return false;
		}
		var year = Integer.parseInt(mainPage.select(".bstar-meta__create-time").text().split(",")[1].trim());
		var alias = "";
		var title = mainPage.select(".bstar-meta__ogv-title").text();
		var episode = mainPage.select(".ep-list > .ep-item").size();

		var alias_ = mainPage.select(".bstar-meta__alias-item");
		alias = String.join(",", alias_.stream().map(Element::text).toList());

		if (type.equals("english")) {
			return year == anilistInfo.getReleaseDate()
					&& Utils.checkNumberEqual(episode, anilistInfo.getCurrentEpisode())
					&& Utils.matchedRate(title, anilistInfo.getTitle().english) > 0.5;
		}

		if (!alias.isEmpty()) {
			String b = String.join(",", anilistInfo.getTitle().nativeTitle, anilistInfo.getTitle().romaji, anilistInfo.getTitle().english);
			return year == anilistInfo.getReleaseDate()
					&& Utils.checkNumberEqual(episode, anilistInfo.getCurrentEpisode())
					&& Utils.matchedRate(alias, b) > 0.6;
		}

		return year == anilistInfo.getReleaseDate()
				&& Utils.checkNumberEqual(episode, anilistInfo.getCurrentEpisode())
				&& Utils.matchedRate(title, anilistInfo.getTitle().romaji) > 0.5;

	}

	private void checkVideoStream(List<VideoResource> videoResources, String value) {
		CompletableFuture.runAsync(() -> {
			var isAllCanAccess = true;
			for (var videoUrl : videoResources) {
				var canAccess = Utils.checkURLBilibili(videoUrl.getUrl());
				if (!canAccess) {
					getLink(value, false);
					isAllCanAccess = false;
					break;
				}
			}
			Logger.info("All video stream can access: " + isAllCanAccess);
		});
	}
}
