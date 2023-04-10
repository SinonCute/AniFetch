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

	private List<String> blacklist;

	private boolean isDebug;

	/*
	 *
	 */

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
		File file = new File(AniFetchApplication.getDataFolder(), "config.yml");
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

		databaseHost = yaml.getString("database.host");
		databasePort = yaml.getString("database.port");
		databaseName = yaml.getString("database.name");
		databaseUser = yaml.getString("database.user");
		databasePassword = yaml.getString("database.password");

		blacklist = yaml.getStringList("blacklist");

		isDebug = yaml.getBoolean("general.debug");

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

	public List<String> getBlacklist() {
		return blacklist;
	}

	public boolean isDebug() {
		return isDebug;
	}

	public String getBilibiliCookie() {
		return bilibiliCookie;
	}

}
