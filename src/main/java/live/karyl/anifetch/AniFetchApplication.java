package live.karyl.anifetch;

import live.karyl.anifetch.connection.OkHttp;
import live.karyl.anifetch.providers.vn.AnimeTVN;
import live.karyl.anifetch.utils.SearchRequest;
import live.karyl.anifetch.utils.Utils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.Arrays;

@SpringBootApplication
public class AniFetchApplication {

	private static OkHttp connection;

	public static void main(String[] args) throws IOException {

		connection = new OkHttp();

		/*var test = new AnimeTVN();
		var anilistInfo = Utils.fetchAnilist("143270");
		var result = test.search(anilistInfo);
		System.out.println("Result id: " + result.getProviderId());
		System.out.println("Episodes: " + result.getEpisodesId().size() + " tập");*/
		var test = SearchRequest.webLinhTinh("Kimi no Na wa");
		System.out.println(Arrays.toString(test));


		SpringApplication.run(AniFetchApplication.class, args);
	}

	public static OkHttp getConnection() {
		return connection;
	}
}
