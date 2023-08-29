package live.karyl.anifetch;

import live.karyl.anifetch.config.ConfigManager;
import live.karyl.anifetch.connection.OkHttp;
import live.karyl.anifetch.database.PostgreSQL;
import live.karyl.anifetch.database.Redis;
import live.karyl.anifetch.providers.AnimeProvider;
import live.karyl.anifetch.providers.eng.Gogoanime;
import live.karyl.anifetch.providers.eng.NineAnime;
import live.karyl.anifetch.providers.vn.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class AniFetchApplication {

	private static OkHttp connection;
	private static Redis redis;
	private static PostgreSQL postgreSQL;
	private static ConfigManager config;

	private static final List<AnimeProvider> providers = new ArrayList<>();

	public static void main(String[] args) {

		config = new ConfigManager();
		config.init();

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
		providers.addAll(
				List.of(
						new Bilibili(),
						new AnimeTVN(),
						new WebLinhTinh(),
						new AnimeHay(),
						new AnimeVietsub(),
						new Gogoanime(),
						new NineAnime()
				)
		);
	}

	public static File getDataFolder() {
		Path path = Paths.get(System.getProperty("user.dir"), "config");
		File file = path.toFile();
		if (!file.exists()) file.mkdirs();
		return file;
	}

	public static InputStream getResourceAsStream(String name) {
		return AniFetchApplication.class.getClassLoader().getResourceAsStream(name);
	}

	public static OkHttp getConnection() {
		return connection;
	}

	public static List<AnimeProvider> getProviders() { return providers; }

	public static Redis getRedis() { return redis; }

	public static PostgreSQL getPostgreSQL() { return postgreSQL; }

	public static ConfigManager getConfig() { return config; }
}
