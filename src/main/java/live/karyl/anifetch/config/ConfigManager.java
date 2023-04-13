package live.karyl.anifetch.config;

import live.karyl.anifetch.AniFetchApplication;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class ConfigManager {

	private static ConfigManager instance;

	private String databaseHost;
	private String databasePort;
	private String databaseName;
	private String databaseUser;
	private String databasePassword;
	private int databaseTimeout;

	private String redisHost;
	private int redisPort;
	private int redisPoolSizeMax;
	private int redisPoolSizeMin;
	private int redisPoolSizeIdle;

	private List<String> blacklist;

	private boolean isDebug;
	private int okHttpRetry;
	private String proxyVN;
	private String userAgent;

	private String bilibiliCookie;

	public static ConfigManager getInstance() {
		return instance;
	}

	public void saveDefaultConfig() {
		if (!AniFetchApplication.getDataFolder().exists()) {
			AniFetchApplication.getDataFolder().mkdir();
		}

		File file = new File(AniFetchApplication.getDataFolder(), "config.yml");

		if (!file.exists()) {
			try (InputStream in = AniFetchApplication.getResourceAsStream("config.yml")) {
				Files.copy(in, file.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void init() {
		saveDefaultConfig();
		File file = new File(AniFetchApplication.getDataFolder(), "config.yml");
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

		databaseHost = yaml.getString("database.host");
		databasePort = yaml.getString("database.port");
		databaseName = yaml.getString("database.name");
		databaseUser = yaml.getString("database.user");
		databasePassword = yaml.getString("database.password");
		databaseTimeout = yaml.getInt("database.timeout");

		redisHost = yaml.getString("redis.host");
		redisPort = yaml.getInt("redis.port");
		redisPoolSizeMax = yaml.getInt("redis.pool.size.max");
		redisPoolSizeMin = yaml.getInt("redis.pool.size.min");
		redisPoolSizeIdle = yaml.getInt("redis.pool.size.idle");

		blacklist = yaml.getStringList("blacklist");

		isDebug = yaml.getBoolean("general.debug");
		okHttpRetry = yaml.getInt("general.okHttpRetry");
		proxyVN = yaml.getString("general.proxyVN");
		userAgent = yaml.getString("general.userAgent");

		bilibiliCookie = yaml.getString("bilibili.cookie");

		instance = this;
	}

	public String getDatabaseHost() {
		return databaseHost;
	}

	public String getDatabasePort() {
		return databasePort;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public String getDatabaseUser() {
		return databaseUser;
	}

	public String getDatabasePassword() {
		return databasePassword;
	}

	public int getDatabaseTimeout() { return databaseTimeout; }

	public String getRedisHost() {
		return redisHost;
	}

	public int getRedisPort() {
		return redisPort;
	}

	public int getRedisPoolSizeMax() {
		return redisPoolSizeMax;
	}

	public int getRedisPoolSizeMin() {
		return redisPoolSizeMin;
	}

	public int getRedisPoolSizeIdle() {
		return redisPoolSizeIdle;
	}

	public List<String> getBlacklist() {
		return blacklist;
	}

	public boolean isDebug() {
		return isDebug;
	}

	public int getOkHttpRetry() {
		return okHttpRetry;
	}

	public String getProxyVN() {
		return proxyVN;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public String getBilibiliCookie() {
		return bilibiliCookie;
	}

}
