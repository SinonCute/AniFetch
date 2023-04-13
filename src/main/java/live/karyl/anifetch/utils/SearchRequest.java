package live.karyl.anifetch.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import live.karyl.anifetch.AniFetchApplication;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class SearchRequest {

	private static final String PROXY_VN = "http://100.71.226.17:8080/v1/server/proxy";
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36";

	public static String[] animeTVN(String key, String year, String[] token) {
		try {

			if (key == null) return new String[0];

			String searchURL = "https://animetvn.xyz/tim-kiem-nang-cao.html?" +
					"q=" + URLEncoder.encode(key, StandardCharsets.UTF_8) +
					"&nam=" + year;
			Request request = new Request.Builder()
					.url(searchURL)
					.addHeader("x-csrf-token", token[0])
					.addHeader("Cookie", token[1])
					.addHeader("x-requested-with", "XMLHttpRequest")
					.build();
			Response response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
			if (response.code() != 200) {
				System.out.println("Request failed");
				return null;
			}
			Document doc = Jsoup.parse(response.body().string());
			Elements elements = doc.select(".film-list > .film_item > .film_item_inner > a");

			if (elements.size() == 0) {
				System.out.println("No result on animeTVN, trying to search with ajax");
				RequestBody requestBody = new FormBody.Builder()
						.addEncoded("key", key)
						.build();
				request = new Request.Builder()
						.url("https://animetvn.xyz/ajax/search")
						.addHeader("x-csrf-token", token[0])
						.addHeader("Cookie", token[1])
						.addHeader("x-requested-with", "XMLHttpRequest")
						.post(requestBody)
						.build();
				response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
				doc = Jsoup.parse(response.body().string());
				elements = doc.select(".search-list > .item > .image");
			}

			System.out.println(elements.size() + " results on animeTVN");
			return elements.stream().map(element -> element.attr("href")).toArray(String[]::new);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String[] webLinhTinh(String key) {
		try {
			String searchURL = "https://weblinhtinh.net/wp-admin/admin-ajax.php";

			if (key == null) return new String[0];

			RequestBody requestBody = new FormBody.Builder()
					.addEncoded("action", "halimthemes_ajax_search")
					.addEncoded("search", key.replaceAll("[^a-zA-Z\\s]", ""))
					.build();
			Request request = new Request.Builder()
					.url(searchURL)
					.post(requestBody)
					.addHeader("x-requested-with", "XMLHttpRequest")
					.build();
			Response response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
			if (response.code() != 200) {
				System.out.println("Request failed");
				return null;
			}
			Document doc = Jsoup.parse(response.body().string());
			Elements elements = doc.select(".exact_result > a");
			System.out.println(elements.size() + " results on webLinhTinh" );
			return elements.stream().map(element -> element.attr("href")).toArray(String[]::new);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String[] animeHay(String key) {
		try {
			if (key == null) return new String[0];
			String keyEncoded = URLEncoder.encode(key.replaceAll(" ", "-"), StandardCharsets.UTF_8);
			String searchURL = "https://animehay.live/tim-kiem/" + keyEncoded + ".html";
			RequestBody requestBody = new FormBody.Builder()
					.addEncoded("url", searchURL)
					.addEncoded("method", "GET")
					.addEncoded("header-user-agent", USER_AGENT)
					.build();
			Request request = new Request.Builder()
					.url(PROXY_VN)
					.post(requestBody)
					.build();

			Response response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
			if (response.code() != 200) {
				System.out.println("Request failed");
				return null;
			}
			Document doc = Jsoup.parse(response.body().string());
			Elements elements = doc.select(".movies-list > .movie-item > a");
			System.out.println(elements.size() + " results on AnimeHay");
			return elements.stream().map(element -> element.attr("href")).toArray(String[]::new);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String[] animeVietsub(String key) {
		try {
			String searchURL = "https://animevietsub.in/ajax/suggest";

			if (key == null) return new String[0];

			RequestBody requestBody = new FormBody.Builder()
					.addEncoded("url", searchURL)
					.addEncoded("method", "POST")
					.addEncoded("body-ajaxSearch", "1")
					.addEncoded("body-keysearch", key)
					.addEncoded("header-x-requested-with", "XMLHttpRequest")
					.addEncoded("header-user-agent", USER_AGENT)
					.build();
			Request request = new Request.Builder()
					.url(PROXY_VN)
					.post(requestBody)
					.build();

			Response response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
			if (response.code() != 200) {
				System.out.println("Request failed");
				return null;
			}

			Document doc = Jsoup.parse(response.body().string());
			Elements elements = doc.select(".ss-info > a");
			System.out.println(elements.size() + " results on animeVietsub" );
			return elements.stream().map(element -> element.attr("href")).toArray(String[]::new);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String[] bilibili(String key) {
		try {
			//Dev bilibili ngu vailoz, can tao day thuat toan khong?
			if (key == null) return new String[0];
			key = key.toLowerCase(Locale.ROOT);
			String keyTrimmed = key.substring(0, key.length() / 2);
			String suggestURL = "https://api.bilibili.tv/intl/gateway/web/v2/search_v2/suggest?s_locale=en_US&platform=web&keyword=" + keyTrimmed;
			String searchURL = "https://api.bilibili.tv/intl/gateway/web/v2/search_v2/anime?s_locale=en_US&platform=web&keyword=%s&highlight=1&pn=1&ps=20".formatted(key);
			RequestBody requestBody = new FormBody.Builder()
					.addEncoded("url", suggestURL)
					.addEncoded("method", "GET")
					.addEncoded("header-user-agent", USER_AGENT)
					.build();
			Request request = new Request.Builder()
					.url(PROXY_VN)
					.post(requestBody)
					.build();
			Response response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
			var jsonObjectSuggest = new Gson().fromJson(response.body().string(), JsonObject.class);
			if (jsonObjectSuggest.getAsJsonObject("data").get("items").isJsonNull()) {
				System.out.println("No suggestion found on bilibili so skip searching to save time");
				return new String[0];
			}
			requestBody = new FormBody.Builder()
					.addEncoded("url", searchURL)
					.addEncoded("method", "GET")
					.addEncoded("header-user-agent", USER_AGENT)
					.build();
			request = new Request.Builder()
					.url(PROXY_VN)
					.post(requestBody)
					.build();
			response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
			if (response.code() != 200) {
				System.out.println("Request failed");
				return null;
			}
			var jsonObject = new Gson().fromJson(response.body().string(), JsonObject.class);
			if (jsonObject.getAsJsonObject("data").get("items").isJsonNull()) return new String[0];
			var itemsArray = jsonObject.getAsJsonObject("data").getAsJsonArray("items");
			String[] result = new String[itemsArray.size()];
			for (int i = 0; i < itemsArray.size(); i++) {
				result[i] = itemsArray.get(i).getAsJsonObject().get("season_id").getAsString();
			}
			System.out.println(result.length + " results on bilibili");
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
