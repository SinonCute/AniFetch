package live.karyl.anifetch.providers.vn;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import live.karyl.anifetch.models.*;
import live.karyl.anifetch.providers.AnimeProvider;
import live.karyl.anifetch.types.AudioType;
import live.karyl.anifetch.types.SubtitleType;
import live.karyl.anifetch.types.VideoType;
import live.karyl.anifetch.utils.SearchRequest;
import live.karyl.anifetch.utils.Utils;
import okhttp3.FormBody;
import okhttp3.Request;
import org.jsoup.nodes.Document;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnimeHay extends AnimeProvider {

	private static final String PLAYER_API = "https://suckplayer.xyz/player/index.php?data=%s&do=getVideo";

	public AnimeHay() {
		super("AnimeHay", "AH","https://animehay.fan/");
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
			var episodes = extractEpisodeIds(baseUrl + "thong-tin-phim/a-" + id + ".html");
			animeParser = new AnimeParser(anilistInfo.getId(), id, siteId, siteName);
			animeParser.setEpisodes(episodes);
			redis.set(redisId, animeParser.toJson(), REDIS_SEARCH);
			return animeParser;
		}

		for (var entry : titles.entrySet()) {
			var title = entry.getValue();
			if (animeParser != null) continue;
			var searchResults = SearchRequest.animeHay(title);
			if (searchResults == null) continue;
			for (var searchResult : searchResults) {
				var mainPage = connect(searchResult, siteName);
				if (compareResult(anilistInfo, mainPage, entry.getKey())) {
					var id = searchResult.replaceAll("^.*-(\\d+)\\.html$", "$1");
					var episodes = extractEpisodeIds(searchResult);
					animeParser = new AnimeParser(anilistInfo.getId(), id, siteId, siteName);
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
		AnimeSource animeSource = new AnimeSource(siteName);
		String redisId = siteName + "$" + value;

		if (redis.exists(redisId, REDIS_SOURCE) && !ignoreCache) {
			String jsonData = redis.get(redisId, REDIS_SOURCE);
			return new Gson().fromJson(jsonData, AnimeSource.class);
		}

		var mainPage = connect(baseUrl + "xem-phim/a-" + value + ".html", siteName);
		Pattern p = Pattern.compile("(?i)(?<=['\"(])(https?://\\S+)(?=['\")])");
		Matcher m = p.matcher(mainPage.html());
		while (m.find()) {
			var link = m.group(1);
			if (link.contains("suckplayer")) {
				var data = firePlayer(link);
				if (data.length == 0) continue;
				var videoResource = new VideoResource(data[0], "720P", "suckplayer", VideoType.HLS);
				videoResource.setUseHeader(true);
				animeSource.addHeader("Cookie", data[1]);
				animeSource.addVideoResource(videoResource);
			}

			if (link.contains("cdninstagram.com")) {
				var videoResource = new VideoResource(link, "720P", "instagram", VideoType.MP4);
				videoResource.setUseHeader(false);
				animeSource.addVideoResource(videoResource);
			}
		}
		animeSource.setAudioType(AudioType.HARD);
		animeSource.setSubtitleType(SubtitleType.HARD);

		redis.set(redisId, animeSource.toJson(), REDIS_SOURCE);
		return animeSource;
	}

	private boolean compareResult(AnilistInfo anilistInfo, Document mainPage, String type) {
		if (mainPage == null) return false;
		var episodeElement = mainPage.select("div.ah_content > div.info-movie > " +
				"div.body > div.list_episode.ah-frame-bg > div.list-item-episode.scroll-bar > a");
		var yearElement = mainPage.select(".update_time > div:nth-child(2)");
		String title = mainPage.select(".heading_movie").text();
		int year = Integer.parseInt(yearElement.text());
		int episode;

		if (episodeElement.size() == 1) {
			episode = 1;
		} else {
			episode = episodeElement.size();
		}

		if (type.equals("english")) {
			return year == anilistInfo.getReleaseDate()
					&& Utils.checkNumberEqual(episode, anilistInfo.getCurrentEpisode())
					&& Utils.matchedRate(title, anilistInfo.getTitle().english) > 0.5;
		}

		return year == anilistInfo.getReleaseDate()
				&& Utils.checkNumberEqual(episode, anilistInfo.getCurrentEpisode())
				&& Utils.matchedRate(title, anilistInfo.getTitle().romaji) > 0.5;
	}

	private List<AnimeEpisode> extractEpisodeIds(String link) {
		Document mainPage = connect(link, siteName);
		List<AnimeEpisode> episodes = new ArrayList<>();
		if (mainPage == null) return new ArrayList<>();
		var episodeElement = mainPage.select("div.ah_content > div.info-movie > " +
				"div.body > div.list_episode.ah-frame-bg > div.list-item-episode.scroll-bar > a");

		// reverse order
		for (int i = episodeElement.size() - 1; i >= 0; i--) {
			var element = episodeElement.get(i);
			var id = element.attr("href").replaceAll("^.*-(\\d+)\\.html$", "$1");
			var number = extractNumberFromString(element.text());
			episodes.add(new AnimeEpisode(element.text(), number, id));
		}
		return episodes;
	}

	private String[] firePlayer(String src) {
		try {
			var id = src.split("/")[4];
			var body = new FormBody.Builder()
					.add("r", baseUrl)
					.add("hash", id)
					.build();
			var request = new Request.Builder()
					.url(String.format(PLAYER_API, id))
					.post(body)
					.addHeader("user-agent", USER_AGENT)
					.addHeader("X-Requested-With", "XMLHttpRequest")
					.build();
			var response = connection.callWithoutRateLimit(request);
			var token = response.header("Set-Cookie").split(";")[0];
			var jsonObject = new Gson().fromJson(response.body().string(), JsonObject.class);
			var link = jsonObject.get("videoSource").getAsString();
			response.close();
			return new String[]{link, token};
		} catch (Exception e) {
			Logger.error(e);
			return new String[]{};
		}
	}
}
