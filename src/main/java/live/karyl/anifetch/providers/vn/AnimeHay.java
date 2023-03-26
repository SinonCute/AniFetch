package live.karyl.anifetch.providers.vn;

import com.google.gson.Gson;
import live.karyl.anifetch.models.AnilistInfo;
import live.karyl.anifetch.models.AnimeEpisode;
import live.karyl.anifetch.models.AnimeParser;
import live.karyl.anifetch.models.AnimeSource;
import live.karyl.anifetch.providers.AnimeProvider;
import live.karyl.anifetch.utils.SearchRequest;
import live.karyl.anifetch.utils.Utils;
import okhttp3.FormBody;
import okhttp3.Request;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
		super("AnimeHay", "https://animehay.live/");
	}

	@Override
	public AnimeParser search(AnilistInfo anilistInfo) {
		AnimeParser animeParser = null;
		String redisId = siteName + "$" + anilistInfo.getId();
		Map<String, String> titles = new HashMap<>();
		titles.put("english", anilistInfo.getTitle().english);
		titles.put("romaji", anilistInfo.getTitle().romaji);

		if (redis.exists(redisId, "search")) {
			animeParser = new Gson().fromJson(redis.get(redisId, "search"), AnimeParser.class);
			if (animeParser != null) return animeParser;
		}

		if (postgreSQL.checkAnimeFetchExists(anilistInfo.getId(), siteName)) {
			var id = postgreSQL.getAnimeFetch(anilistInfo.getId(), siteName);
			var episodes = extractEpisodeIds(PROXY_VN + "https://animehay.live/thong-tin-phim/a-" + id + ".html");
			animeParser = new AnimeParser(anilistInfo.getId(), id, siteName);
			animeParser.setEpisodes(episodes);
			redis.set(redisId, animeParser.toJson(), "search");
			return animeParser;
		}

		for (var key : titles.keySet()) {
			var title = titles.get(key);
			if (animeParser != null) break;
			var searchResults = SearchRequest.animeHay(title);
			if (searchResults == null) continue;
			for (var searchResult : searchResults) {
				var mainPage = Utils.connect(PROXY_VN + searchResult);
				System.out.println("debug:" + searchResult);
				if (compareResult(anilistInfo, mainPage, key)) {
					var id = searchResult.replaceAll("^.*-(\\d+)\\.html$", "$1");
					var episodes = extractEpisodeIds(searchResult);
					animeParser = new AnimeParser(anilistInfo.getId(), id, siteName);
					animeParser.setEpisodes(episodes);
					redis.set(redisId, animeParser.toJson(), "search");
					postgreSQL.addAnimeFetch(animeParser);
					break;
				}
			}
		}
		return animeParser;
	}

	@Override
	public AnimeSource getLink(String value) {
		AnimeSource animeSource = new AnimeSource(siteName);
		String redisId = siteName + "$" + value;

		if (redis.exists(redisId, "source")) {
			String jsonData = redis.get(redisId, "source");
			return new Gson().fromJson(jsonData, AnimeSource.class);
		}

		var mainPage = Utils.connect(PROXY_VN + "https://animehay.live/xem-phim/a-" + value + ".html");
		Pattern p = Pattern.compile("(?i)(?<=['\"(])(https?://\\S+)(?=['\")])");
		Matcher m = p.matcher(mainPage.html());
		while (m.find()) {
			var link = m.group(1);
			if (link.contains("suckplayer")) {
				var data = firePlayer(link);
				if (data != null) {
					var source = new AnimeSource.Source(data[0], "suckplayer", "hls");
					source.addHeader(data[1]);
					animeSource.addSource(source);
				}
			}
			if (link.contains("cdninstagram.com")) {
				animeSource.addSource(link, "facebook", "mp4");
			}
		}
		redis.set(redisId, animeSource.toJson(), "source");
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
		Document mainPage = Utils.connect(PROXY_VN + link);
		List<AnimeEpisode> episodes = new ArrayList<>();
		if (mainPage == null) return null;
		var episodeElement = mainPage.select("div.ah_content > div.info-movie > " +
				"div.body > div.list_episode.ah-frame-bg > div.list-item-episode.scroll-bar > a");

		// reverse order
		for (int i = episodeElement.size() - 1; i >= 0; i--) {
			var element = episodeElement.get(i);
			var id = element.attr("href").replaceAll("^.*-(\\d+)\\.html$", "$1");
			var number = extractNumberFromString(element.text());
			episodes.add(new AnimeEpisode(number, id));
		}
		return episodes;
	}

	private String[] firePlayer(String src) {
		try {
			var id = src.split("/")[4];
			var request = new Request.Builder()
					.url(String.format(PLAYER_API, id))
					.addHeader("user-agent", USER_AGENT)
					.build();
			var response = connection.callWithoutRateLimit(request);
			var token = response.headers().get("Set-Cookie").split("=")[1].split(";")[0];
			response.close();
			var body = new FormBody.Builder()
					.add("hash", id)
					.build();
			request = new Request.Builder()
					.url(String.format(PLAYER_API, id))
					.addHeader("Cookie", "fireplayer_player=" + token)
					.addHeader("x-requested-with", "XMLHttpRequest")
					.addHeader("user-agent", USER_AGENT)
					.post(body)
					.build();
			response = connection.callWithoutRateLimit(request);

			// parse json
			JSONParser jsonParser = new JSONParser();
			JSONObject links = (JSONObject) jsonParser.parse(response.body().string());
			var link = links.get("videoSource").toString();
			response.close();
			return new String[]{link, "Cookie: fireplayer=" + token};
		} catch (Exception e) {
			Logger.error(e);
			return null;
		}
	}
}
