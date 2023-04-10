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
import okhttp3.RequestBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimeVietsub extends AnimeProvider {

	public AnimeVietsub() {
		super("AnimeVietsub", "AVS","https://animevietsub.in/");
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
			var episodes = extractEpisodeIds("https://animevietsub.in/phim/a-a" + id + "/");
			animeParser = new AnimeParser(anilistInfo.getId(), id, siteId, siteName);
			animeParser.setEpisodes(episodes);
			redis.set(redisId, animeParser.toJson(), REDIS_SEARCH);
			return animeParser;
		}

		for (var entry : titles.entrySet()) {
			var title = entry.getValue();
			if (animeParser != null) continue;
			var searchResults = SearchRequest.animeVietsub(title);
			if (searchResults == null) continue;
			for (var searchResult : searchResults) {
				var mainPage = connect(searchResult, siteName);
				if (compareResult(anilistInfo, mainPage, entry.getKey())) {
					var id = searchResult.replaceAll(".+a(\\d+)/", "$1");
					var episodes = extractEpisodeIds(searchResult);
					animeParser = new AnimeParser(anilistInfo.getId(), id, siteId, siteName);
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
			String animeId = value.split("\\$")[0];
			String episodeId = value.split("\\$")[1];
			AnimeSource animeSource = new AnimeSource(siteName);
			String redisId = siteName + "$" + episodeId;

			if (redis.exists(redisId, REDIS_SOURCE)) {
				String jsonData = redis.get(redisId, REDIS_SOURCE);
				return new Gson().fromJson(jsonData, AnimeSource.class);
			}

			RequestBody requestBody = new FormBody.Builder()
					.addEncoded("url", "https://animevietsub.in/ajax/player?v=2019a")
					.addEncoded("method", "POST")
					.addEncoded("body-episodeId", episodeId)
					.addEncoded("body-backup", "1")
					.addEncoded("header-x-requested-with", "XMLHttpRequest")
					.addEncoded("header-user-agent", USER_AGENT)
					.build();
			Request request = new Request.Builder()
					.url(PROXY_VN)
					.post(requestBody)
					.build();
			var response = connection.callWithoutRateLimit(request);

			var json = new Gson().fromJson(response.body().string(), JsonObject.class);
			var data = json.get("html").getAsString();
			var doc = Jsoup.parse(data);
			doc.select(".btn3dsv").forEach(element -> {
				try {
					var dataLink = element.attr("data-href");
					RequestBody requestBodyLink = new FormBody.Builder()
							.addEncoded("url", "https://animevietsub.in/ajax/player?v=2019a")
							.addEncoded("method", "POST")
							.addEncoded("body-link", dataLink)
							.addEncoded("body-id", animeId)
							.addEncoded("header-x-requested-with", "XMLHttpRequest")
							.addEncoded("header-user-agent", USER_AGENT)
							.build();

					Request requestLink = new Request.Builder()
							.url(PROXY_VN)
							.post(requestBodyLink)
							.build();
					var responseLink = connection.callWithoutRateLimit(requestLink);
					var jsonInfo = new Gson().fromJson(responseLink.body().string(), JsonObject.class);
					var jsonLink = jsonInfo.get("link").isJsonArray() ? jsonInfo.get("link").getAsJsonArray().get(0).getAsJsonObject() : null;
					if (jsonLink == null || !jsonLink.has("type")) return;
					if (jsonLink.get("type").getAsString().equals("hls")) {
						var link = jsonLink.get("file").getAsString();
						link = link.replace("//", "https://");
						var videoResource = new VideoResource(link, "720P", "DU", VideoType.HLS);
						videoResource.setUseHeader(true);
						animeSource.addHeader("Referer", new String[]{"https://animevietsub.in/"});
						animeSource.addHeader("Origin", new String[]{"https://animevietsub.in/"});
						animeSource.addVideoResource(videoResource);
					}
				} catch (IOException ignored) {}
			});
			animeSource.setSubtitleType(SubtitleType.HARD);
			animeSource.setAudioType(AudioType.HARD);

			redis.set(value, animeSource.toJson(), REDIS_SOURCE);
			return animeSource;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private List<AnimeEpisode> extractEpisodeIds(String link) {
		Document mainPage = connect(link + "xem-phim.html", siteName);
		var id = link.replaceAll(".+a(\\d+)/", "$1");
		if (mainPage == null) return null;
		List<AnimeEpisode> episodes = new ArrayList<>();
		mainPage.select(".list-episode > .episode > a").forEach(element -> {
			var episodeId = element.attr("data-id");
			var labelEpisode = element.text();
			var number = extractNumberFromString(labelEpisode);
			episodes.add(new AnimeEpisode(labelEpisode, number, id + "$" + episodeId));
		});
		return episodes;
	}

	private boolean compareResult(AnilistInfo anilistInfo, Document mainPage, String type) {
		if (mainPage == null) return false;
		var info = mainPage.select(".Single > .ClFx > .Info");
		var title = mainPage.select(".Single > header > .Title").text() + "," +
				mainPage.select(".Single > header > .SubTitle").text();
		var year = info.select(".AAIco-date_range").text();
		var episode = info.select(".AAIco-access_time").text().split("/")[0];
		// những phần mới chưa chiếu thì không có số tập
		if (episode.matches(".*\\D+.*")) return false;

		if (type.equals("english")) {
			return Utils.checkNumberEqual(Integer.parseInt(episode), anilistInfo.getCurrentEpisode())
					&& Utils.matchedRate(title, anilistInfo.getTitle().english) > 0.5
					&& Utils.checkNumberEqual(Integer.parseInt(year), anilistInfo.getStartDate().year);
		}

		return Utils.checkNumberEqual(Integer.parseInt(episode), anilistInfo.getCurrentEpisode())
				&& Utils.matchedRate(title, anilistInfo.getTitle().romaji) > 0.5
				&& Utils.checkNumberEqual(Integer.parseInt(year), anilistInfo.getStartDate().year);
	}
}
