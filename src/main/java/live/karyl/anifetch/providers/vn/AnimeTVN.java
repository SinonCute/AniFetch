package live.karyl.anifetch.providers.vn;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import okhttp3.Response;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnimeTVN extends AnimeProvider {

	private static final String ANIME_TVN_USER_ID = "61211fa59c0163458e94b0c0";
	private final String[] token;
	private long tokenLifeTime;


	public AnimeTVN() {
		super("AnimeTVN", "ATVN","https://animetvn.in/");
		token = new String[2];
		tokenLifeTime = System.currentTimeMillis() + Duration.ofHours(12).toMillis();
		requestToken();
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
			var episodes = extractEpisodeIds(baseUrl + "thong-tin-phim/f" + id + "-a.html");
			animeParser = new AnimeParser(anilistInfo.getId(), id,  siteId, siteName);
			animeParser.setEpisodes(episodes);
			redis.set(redisId, animeParser.toJson(), REDIS_SEARCH);
			return animeParser;
		}

		for (var entry : titles.entrySet()) {
			String title = entry.getValue();
			if (animeParser != null) break;
			if (title == null) continue;
			var searchResults = SearchRequest.animeTVN(title, anilistInfo.getReleaseDate() + "", token);
			if (searchResults == null) continue;
			for (var searchResult : searchResults) {
				if (compareResult(anilistInfo, searchResult, entry.getKey())) {
					String id = searchResult.replaceAll("^.*f(\\d+).*$", "$1");
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
		try {
			AnimeSource animeSource = new AnimeSource(siteName);
			String redisId = siteName + "$" + value;

			if (redis.exists(redisId, REDIS_SOURCE) && !ignoreCache) {
				String jsonData = redis.get(redisId, REDIS_SOURCE);
				return new Gson().fromJson(jsonData, AnimeSource.class);
			}

			String listServer = requestPostGetLink(value, true);
			JsonObject links = new Gson().fromJson(listServer, JsonObject.class);
			JsonArray servers = links.get("links").getAsJsonArray();

			for (var server : servers) {
				JsonObject serverValue = server.getAsJsonObject();
				String id = serverValue.get("id").getAsString();
				String epLink = serverValue.get("link").getAsString();

				if (!(id.equals("13") || id.equals("20"))) {
					continue;
				}

				String params = "{0},{1}"
						.replace("{0}", id)
						.replace("{1}", URLEncoder.encode(epLink, StandardCharsets.UTF_8));
				String linkPlayer = requestPostGetLink(params, false);
				String data = new Gson().fromJson(linkPlayer, JsonObject.class).get("link").getAsString();
				switch (id) {
					case "20" -> {
						URI uri = new URI(data);
						String path = uri.getPath();
						String fileID = path.substring(path.lastIndexOf("/") + 1);
						var url = playHQB(fileID);
						if (url != null) {
							var videoResource = new VideoResource(url, "720P", "TVN", VideoType.HLS);
							animeSource.addVideoResource(videoResource);
						}
					}
					case "13" -> {
						var document = connect(data, siteName);
						if (document == null) continue;
						String patternString = "https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
						Pattern pattern = Pattern.compile(patternString);
						Matcher matcher = pattern.matcher(document.html());
						if (matcher.find()) {
							var videoResource = new VideoResource(matcher.group(), "720P", "FB", VideoType.MP4);
							animeSource.addVideoResource(videoResource);
						}
					}
				}
			}
			animeSource.setAudioType(AudioType.HARD);
			animeSource.setSubtitleType(SubtitleType.HARD);

			redis.set(redisId, animeSource.toJson(), REDIS_SOURCE);
			return animeSource;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private List<AnimeEpisode> extractEpisodeIds(String link) {
		var mainPage = connect(link, siteName);
		if (mainPage.select(".play-now").isEmpty()) return null;
		var watchUrl = mainPage.select(".play-now").get(0).attr("href");
		var watchPage = connect(watchUrl, siteName);

		List<AnimeEpisode> episodes = new ArrayList<>();

		for (var server : watchPage.select(".eplist").get(0).getElementsByClass("svep")) {
			if (server.select(".svname").text().equalsIgnoreCase("Trailer")) continue;
			for (var ep : server.select("a.tapphim")) {
				var id = ep.attr("id").split("_")[1];
				var episodeNumber = extractNumberFromString(ep.text());
				episodes.add(new AnimeEpisode(ep.text(), episodeNumber, id));
			}
		}

		episodes.sort(Comparator.comparingInt(AnimeEpisode::episodeNumber));

		return episodes;
	}

	private boolean compareResult(AnilistInfo anilistInfo, String link, String type) {
		Document document = connect(link, siteName);
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
					.addEncoded("referrer", "https://animetvn.in")
					.addEncoded("typeend", "html")
					.build();
			Request request = new Request.Builder()
					.url(url)
					.addHeader("Origin", "https://play.playhbq.xyz")
					.addHeader("Referer", "https://play.playhbq.xyz/")
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
			checkToken();
			if (multiServer) {
				url = baseUrl + "ajax/getExtraLinks";
				req = new FormBody.Builder()
						.addEncoded("epid", data)
						.build();
			} else {
				url = baseUrl + "ajax/getExtraLink";
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

	private void checkToken() {
		if (tokenLifeTime < System.currentTimeMillis()) {
			requestToken();
			tokenLifeTime = System.currentTimeMillis() + Duration.ofHours(12).toMillis();
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