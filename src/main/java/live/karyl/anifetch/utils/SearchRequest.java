package live.karyl.anifetch.utils;

import live.karyl.anifetch.AniFetchApplication;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SearchRequest {

	private static final String PROXY_VN = "https://proxy-vn.karyl.live/v1/server/proxy?link=";
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
					.addEncoded("search", key)
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

			String searchURL = "https://animehay.live/tim-kiem/" + URLEncoder.encode(key.replaceAll(" ", "-"), StandardCharsets.UTF_8) + ".html";
			Request request = new Request.Builder()
					.url(searchURL)
					.addHeader("User-Agent", USER_AGENT)
					.addHeader("Cookie", "cf_clearance=qAUxm5MmaZHZWlgeXtKpgt4A2fcmz8VrMq9dMJBWhII-1678686086-0-160")
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

	public static String[] animeVietsub(String key, String Cookie) {
		try {
			String searchURL = "https://animevietsub.in/ajax/suggest";

			if (key == null) return new String[0];

			RequestBody requestBody = new FormBody.Builder()
					.addEncoded("ajaxSearch", "1")
					.addEncoded("keysearch", key)
					.build();
			Request request = new Request.Builder()
					.url(searchURL)
					.post(requestBody)
					.addHeader("x-requested-with", "XMLHttpRequest")
					.addHeader("user-agent", USER_AGENT)
					.addHeader("Cookie", Cookie)
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
}
