package live.karyl.anifetch;

import live.karyl.anifetch.connection.OkHttp;
import live.karyl.anifetch.utils.SearchRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.Arrays;

@SpringBootApplication
public class AniFetchApplication {

	private static OkHttp connection;

	public static void main(String[] args) throws IOException {

		connection = new OkHttp();

		var a = SearchRequest.animeTVN("naruto");
		System.out.println(Arrays.asList(a));


		SpringApplication.run(AniFetchApplication.class, args);
	}

	public static OkHttp getConnection() {
		return connection;
	}
}
