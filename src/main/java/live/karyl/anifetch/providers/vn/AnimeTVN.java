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
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnimeTVN extends AnimeProvider {

	private static final String ANIME_TVN_USER_ID = "61211fa59c0163458e94b0c0";
	private final String[] token;


	public AnimeTVN() {
		super("AnimeTVN", "https://animetvn.xyz/");
		token = new String[2];
		requestToken();
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
			var episodes = extractEpisodeIds("https://animetvn.xyz/thong-tin-phim/f" + id + "-a.html");
			animeParser = new AnimeParser(anilistInfo.getId(), id, siteName);
			animeParser.setEpisodes(episodes);
			redis.set(redisId, animeParser.toJson(), "search");
			return animeParser;
		}

		for (String key : titles.keySet()) {
			String title = titles.get(key);
			if (animeParser != null) break;
			if (title == null) continue;
			var searchResults = SearchRequest.animeTVN(title, anilistInfo.getReleaseDate() + "", token);
			if (searchResults == null) continue;
			for (var searchResult : searchResults) {
				if (compareResult(anilistInfo, searchResult, key)) {
					String id = searchResult.replaceAll("^.*f(\\d+).*$", "$1");
					var episodes = extractEpisodeIds(searchResult);
					animeParser = new AnimeParser(anilistInfo.getId(), id, siteName);
					animeParser.setEpisodes(episodes);
					postgreSQL.addAnimeFetch(animeParser);
					break;
				}
			}
		}
		if (animeParser == null) return null;
		redis.set(redisId, animeParser.toJson(), "search");
		return animeParser;
	}

	@Override
	public AnimeSource getLink(String value) {
		try {
			AnimeSource sources = new AnimeSource(siteName);
			String redisId = siteName + "$" + value;

			if (redis.exists(redisId, "source")) {
				String jsonData = redis.get(redisId, "source");
				return new Gson().fromJson(jsonData, AnimeSource.class);
			}

			JSONParser jsonParser = new JSONParser();
			JSONObject links = (JSONObject) jsonParser.parse(requestPostGetLink(value, true));
			JSONArray linksArray = (JSONArray) links.get("links");


			for (var object : linksArray) {
				JSONObject linksValue = (JSONObject) object;
				String id = linksValue.get("id").toString();
				String epLink = linksValue.get("link").toString();
				String params = "{0},{1}"
						.replace("{0}", id)
						.replace("{1}", URLEncoder.encode(epLink, StandardCharsets.UTF_8));
				String linkPlayer = requestPostGetLink(params, false);
				String data = (String) ((JSONObject) jsonParser.parse(linkPlayer)).get("link");
				switch (id) {
					case "20" -> {
						URI uri = new URI(data);
						String path = uri.getPath();
						String fileID = path.substring(path.lastIndexOf("/") + 1);
						var a = playHQB(fileID);
						if (a != null) sources.addSource(a,id, "hls");
					}
					case "13" -> {
						var document = Utils.connect(data);
						String patternString = "https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
						Pattern pattern = Pattern.compile(patternString);
						Matcher matcher = pattern.matcher(document.html());
						if (matcher.find()) sources.addSource(matcher.group(), id, "hls");
					}
				}
			}
			redis.set(redisId, sources.toJson(), "source");
			return sources;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private List<AnimeEpisode> extractEpisodeIds(String link) {
		var mainPage = Utils.connect(link);
		if (mainPage.select(".play-now").isEmpty()) return null;
		var watchUrl = mainPage.select(".play-now").get(0).attr("href");
		var watchPage = Utils.connect(watchUrl);

		List<AnimeEpisode> episodes = new ArrayList<>();

		for (var server : watchPage.select(".eplist").get(0).getElementsByClass("svep")) {
			if (server.select(".svname").text().equalsIgnoreCase("Trailer")) continue;
			for (var ep : server.select("a.tapphim")) {
				var id = ep.attr("id").split("_")[1];
				var episodeNumber = extractNumberFromString(ep.text());
				episodes.add(new AnimeEpisode(episodeNumber, id));
			}
		}

		episodes.sort(Comparator.comparingInt(AnimeEpisode::episodeNumber));

		return episodes;
	}

	private boolean compareResult(AnilistInfo anilistInfo, String link, String type) {
		Document document = Utils.connect(link);
		String title = document.select(".name-vi").first().text();
		int year = 0;
		int episode = 0;

		for (var info : document.select(".more-info")) {
			Pattern episodePattern = Pattern.compile("Số tập: </span>(\\d+)");
			Pattern yearPattern = Pattern.compile("Năm phát sóng: </span>(\\w+\\s+\\d{4})");
			Matcher episodeMatcher = episodePattern.matcher(info.html());
			Matcher yearMatcher = yearPattern.matcher(info.html());
			if (episodeMatcher.find()) {
				episode = Integer.parseInt(episodeMatcher.group(1));
			}
			if (yearMatcher.find()) {
				year = Integer.parseInt(yearMatcher.group(1).split(" ")[1]);
			}
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

	public String playHQB(String fileID) {
		try {
			String url = "https://api-plhq.playhbq.xyz/apiv4/" + ANIME_TVN_USER_ID + "/" + fileID;
			RequestBody requestBody = new FormBody.Builder()
					.addEncoded("referrer", "https://animetvn.xyz")
					.addEncoded("typeend", "html")
					.build();
			Request request = new Request.Builder()
					.url(url)
					.addHeader("origin", "https://play.playhbq.xyz")
					.addHeader("referer", "https://play.playhbq.xyz/")
					.post(requestBody)
					.build();
			Response response = connection.callWithoutRateLimit(request);
			if (response.code() != 200) {
				throw new RuntimeException("Request failed");
			}
			JSONObject jsonData = (JSONObject) new JSONParser().parse(response.body().string());
			var link = jsonData.get("data").toString();
			response.close();
			return link;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String requestPostGetLink(String data, boolean multiServer) {
		try {
			String url;
			FormBody req;
			if (multiServer) {
				url = "https://animetvn.xyz/ajax/getExtraLinks";
				req = new FormBody.Builder()
						.addEncoded("epid", data)
						.build();
			} else {
				url = "https://animetvn.xyz/ajax/getExtraLink";
				req = new FormBody.Builder()
						.addEncoded("id", data.split(",")[0])
						.addEncoded("link", data.split(",")[1])
						.build();
			}
			Request request = new Request.Builder()
					.url(url)
					.addHeader("Cookie", token[1])
					.addHeader("x-csrf-token", token[0])
					.post(req)
					.build();
			Response response = connection.callWithoutRateLimit(request);
			if (response.code() != 200) {
				throw new RuntimeException("Request failed");
			}
			return response.body().string();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void requestToken() {
		try {
			Request request = new Request.Builder()
					.url(baseUrl).build();
			Response response = connection.callWithoutRateLimit(request);
			if (response.code() != 200) {
				throw new RuntimeException("Request failed");
			}
			Document doc = Jsoup.parse(response.body().string());
			var laravel_session = response.header("Set-Cookie").split(";")[0];
			var xsrf_tok = response.headers().values("Set-Cookie").get(0).split(";")[0];
			token[0] = doc.selectXpath("/html/head/meta[21]").get(0).attr("content");
			token[1] = laravel_session + "; " + xsrf_tok;
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}