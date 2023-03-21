package live.karyl.anifetch.utils;

import live.karyl.anifetch.AniFetchApplication;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class SearchRequest {

	public static String[] animeTVN(String key) throws IOException {
		String searchURL = "https://animetvn.xyz/ajax/search";

		RequestBody requestBody = new FormBody.Builder()
				.addEncoded("key", key)
				.build();
		Request request = new Request.Builder()
				.url(searchURL)
				.post(requestBody)
				.addHeader("x-csrf-token", "Ork4mj66D7CgqPkqiTsMv35VhsU0nnbyubzIYtgD")
				.addHeader("x-requested-with","XMLHttpRequest")
				.addHeader("Cookie", "XSRF-TOKEN=eyJpdiI6ImFEaFJGWkRRejE2c2d0cGxVYjVBSVE9PSIsInZhbHVlIjoiTmpPOEpoendKMWJMdHc0dFwvVnhwS2lhb04zV3AzZWY1Wkh5czkzZGVFYUQzT2p3eGxTZjM3VWZnNlpQTTE2eTBhb0xRVG85bkJLYlpnM3ArbUREWGNRPT0iLCJtYWMiOiI2YTljYTFhYjNhYzNkOGNkNTFiNGZlYjAxOWViMjdmNjg2MTM1ZWIwYTM4Y2ZmNDNiZGVhYzYxMWFhY2VhNDc1In0%3D; laravel_session=eyJpdiI6InRrRVNSY1JwQ2lGRytHZkRPaHdnOVE9PSIsInZhbHVlIjoiYnRHU2NndUVSblFUekdYTDIydExhSmo5cWRJR2lCdkFyUXBaSjlXNjk4VEg3N3hcL29Jc0RlRzBNVUFtWE8rSE1pTGRmdTdxUzIxQkRicVE3TVdZeGpRPT0iLCJtYWMiOiJiOTQzNDAwMTI5ZjUyZWNkY2IwZGE2NDZhYzg4YzMwNjg3NWZhYjc3NmZiNTUwZmMwNTE0MmYwZTNmOTk1OWMyIn0%3D")
				.build();
		Response response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
		if (response.code() != 200) {
			System.out.println("Request failed");
			return null;
		}
		Document doc = Jsoup.parse(response.body().string());
		Elements elements = doc.select(".search-list > .item > .image");
		return elements.stream().map(element -> element.attr("href")).toArray(String[]::new);
	}
}
