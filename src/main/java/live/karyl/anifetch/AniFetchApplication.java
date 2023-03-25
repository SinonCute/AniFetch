package live.karyl.anifetch;

import live.karyl.anifetch.connection.OkHttp;
import live.karyl.anifetch.database.PostgreSQL;
import live.karyl.anifetch.database.Redis;
import live.karyl.anifetch.providers.AnimeProvider;
import live.karyl.anifetch.providers.vn.AnimeHay;
import live.karyl.anifetch.providers.vn.AnimeTVN;
import live.karyl.anifetch.providers.vn.WebLinhTinh;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class AniFetchApplication {

	private static OkHttp connection;
	private static Redis redis;
	private static PostgreSQL postgreSQL;
	private static final Map<String, AnimeProvider> providers = new HashMap<>();

	public static void main(String[] args) {

		connection = new OkHttp();
		connection.init();

		redis = new Redis();
		redis.init();

		postgreSQL = new PostgreSQL();
		postgreSQL.init();

		init();

		SpringApplication.run(AniFetchApplication.class, args);
	}

	public static void init() {
		providers.put("AnimeTVN", new AnimeTVN());
		providers.put("WebLinhTinh", new WebLinhTinh());
		providers.put("AnimeHay", new AnimeHay());
	}

	public static OkHttp getConnection() {
		return connection;
	}

	public static Map<String, AnimeProvider> getProviders() { return providers; }

	public static Redis getRedis() { return redis; }

	public static PostgreSQL getPostgreSQL() { return postgreSQL; }
}
