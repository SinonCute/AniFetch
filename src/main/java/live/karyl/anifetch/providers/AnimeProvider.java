package live.karyl.anifetch.providers;

import live.karyl.anifetch.AniFetchApplication;
import live.karyl.anifetch.config.ConfigManager;
import live.karyl.anifetch.connection.OkHttp;
import live.karyl.anifetch.database.PostgreSQL;
import live.karyl.anifetch.database.Redis;
import live.karyl.anifetch.models.AnilistInfo;
import live.karyl.anifetch.models.AnimeParser;
import live.karyl.anifetch.models.AnimeSource;
import live.karyl.anifetch.utils.Utils;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public abstract class AnimeProvider {
	protected final String siteName;
	protected final String siteId;
	protected final String baseUrl;

	protected String USER_AGENT = "";
	protected String PROXY_VN = "";

	protected final String REDIS_SEARCH = "search";
	protected final String REDIS_SOURCE = "source";
	protected final String REDIS_NON_EXIST = "non_exist";


	protected final OkHttp connection = AniFetchApplication.getConnection();
	protected final Redis redis = AniFetchApplication.getRedis();
	protected final PostgreSQL postgreSQL = AniFetchApplication.getPostgreSQL();
	protected final ConfigManager config = AniFetchApplication.getConfig();

	protected AnimeProvider(String siteName, String siteId, String baseUrl) {
		this.siteName = siteName;
		this.siteId = siteId;
		this.baseUrl = baseUrl;
		PROXY_VN = config.getProxyVN();
		USER_AGENT = config.getUserAgent();
	}

	public abstract AnimeParser search(AnilistInfo anilistInfo);

	public abstract AnimeSource getLink(String value, boolean ignoreCache);

	protected Document connect(String url, String siteName) {
		switch (siteName) {
			case "AnimeHay", "AnimeVietsub" -> {
				try {
					RequestBody requestBody = new FormBody.Builder()
							.addEncoded("url", url)
							.addEncoded("method", "GET")
							.addEncoded("header-user-agent", USER_AGENT)
							.build();
					Request request = new Request.Builder()
							.url(PROXY_VN)
							.post(requestBody)
							.build();
					Response response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
					return Jsoup.parse(new String(response.body().bytes(), StandardCharsets.UTF_8));
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
			case "Bilibili" -> {
				try {
					RequestBody requestBody = new FormBody.Builder()
							.addEncoded("url", url)
							.addEncoded("method", "GET")
							.addEncoded("header-user-agent", USER_AGENT)
							.build();
					Request request = new Request.Builder()
							.url(config.getProxyVNBackup())
							.post(requestBody)
							.build();
					Response response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
					return Jsoup.parse(new String(response.body().bytes(), StandardCharsets.UTF_8));
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
			default -> {
				return Utils.connect(url, "");
			}
		}
	}

	protected static int extractNumberFromString(String s) {
        s = s.toLowerCase(Locale.ROOT);
        if (s.contains("-")) {
            return Integer.parseInt(s.split("-")[0]);
        } else if (s.endsWith("_end")) {
            return Integer.parseInt(s.replace("_end", ""));
        } else if (s.contains(".")) {
	        return Integer.parseInt(Math.round(Double.parseDouble(s)) + "");
        } else if (s.contains("full")) {
            return 1;
        } else if (s.startsWith("e")) {
	        return Integer.parseInt(s.replace("e", ""));
        } else if (s.contains("_")) {
			return Integer.parseInt(s.split("_")[0]);
		} else {
			return Integer.parseInt(s);
		}
    }

	public String getSiteName() { return siteName; }

	public String getSiteId() { return siteId; }
}
