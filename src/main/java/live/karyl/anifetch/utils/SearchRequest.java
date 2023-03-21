package live.karyl.anifetch.utils;

import live.karyl.anifetch.AniFetchApplication;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class SearchRequest {

	public static String[] animeTVN(String key) {
		try {
			String searchURL = "https://animetvn.xyz/ajax/search";

			RequestBody requestBody = new FormBody.Builder()
					.addEncoded("key", key)
					.build();
			Request request = new Request.Builder()
					.url(searchURL)
					.post(requestBody)
					.addHeader("x-csrf-token", "L8l9OmdJWvtdoJos6mOGzkhKTVkHZyQhUNISxSvK")
					.addHeader("x-requested-with", "XMLHttpRequest")
					.addHeader("Cookie", "XSRF-TOKEN=eyJpdiI6InZMMFZuS2lONjVjTW1RK1d3TU0yTkE9PSIsInZhbHVlIjoiV2N0cWQzMURYamVMOExYdVp1Rk1IM3BDTG1XajVYOHpjZ29sRGMwWG9HK3dsR0FPc2IzRGFoc2ZUVFNqUFVNajcyKyt5Wm02WjZOTmVxdnE4Y3JFM2c9PSIsIm1hYyI6Ijk2MDI5ZTVmNDM0ZWE2YWY5NjRjN2E1MDAyYzg0YmEzOWE0MmE1NWU0YjY2OTdlMjJhOWNiODdiY2JiMzRmZjkifQ%3D%3D; laravel_session=eyJpdiI6IlNKMEU1eG9tWHhuMTdGMW1WSTd0eEE9PSIsInZhbHVlIjoic28yZXZIbEpuUVE5ZFAybzNnUElDNjFBS2phMmhkQ2lCVHVqRlpOUTRqVFFid2QxN213NkI0VmtscEpaQU9QYmhTVnlXdTM3MlNGamxONUV2YTNockE9PSIsIm1hYyI6ImQyMzEzMWM5MDZkOTNlMjdhYzEzZTAyZjFjYjllYTU3MTdjZmFmNTZkODY5ZDgxNmQ4YjcwMzIwMTUxOTg4NjUifQ%3D%3D")
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
