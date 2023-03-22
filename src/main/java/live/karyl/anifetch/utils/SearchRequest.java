package live.karyl.anifetch.utils;

import live.karyl.anifetch.AniFetchApplication;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class SearchRequest {

	public static String[] animeTVN(String key, String[] token) {
		try {
			String searchURL = "https://animetvn.xyz/ajax/search";

			RequestBody requestBody = new FormBody.Builder()
					.addEncoded("key", key)
					.build();
			Request request = new Request.Builder()
					.url(searchURL)
					.post(requestBody)
					.addHeader("x-csrf-token", token[0])
					.addHeader("x-requested-with", "XMLHttpRequest")
					.addHeader("Cookie", token[1])
					.build();
			Response response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
			if (response.code() != 200) {
				System.out.println("Request failed");
				return null;
			}
			Document doc = Jsoup.parse(response.body().string());
			Elements elements = doc.select(".search-list > .item > .image");
			System.out.println(elements.size() + " results");
			return elements.stream().map(element -> element.attr("href")).toArray(String[]::new);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String[] webLinhTinh(String key) {
		try {
			String searchURL = "https://weblinhtinh.net/wp-admin/admin-ajax.php";

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
			System.out.println(elements.size() + " results");
			return elements.stream().map(element -> element.attr("href")).toArray(String[]::new);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
