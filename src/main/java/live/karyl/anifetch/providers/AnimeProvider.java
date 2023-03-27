package live.karyl.anifetch.providers;

import live.karyl.anifetch.AniFetchApplication;
import live.karyl.anifetch.connection.OkHttp;
import live.karyl.anifetch.database.PostgreSQL;
import live.karyl.anifetch.database.Redis;
import live.karyl.anifetch.models.AnilistInfo;
import live.karyl.anifetch.models.AnimeParser;
import live.karyl.anifetch.models.AnimeSource;
import live.karyl.anifetch.utils.Utils;

import java.util.List;
import java.util.Locale;

public abstract class AnimeProvider {
	protected final String siteName;
	protected final String baseUrl;

	protected final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.134 Safari/537.36";
	protected final String PROXY_VN = "https://proxy-vn.karyl.live/v1/server/proxy?link=";

	protected final OkHttp connection = AniFetchApplication.getConnection();
	protected final Redis redis = AniFetchApplication.getRedis();
	protected final PostgreSQL postgreSQL = AniFetchApplication.getPostgreSQL();

	public AnimeProvider(String siteName, String baseUrl) {
		this.siteName = siteName;
		this.baseUrl = baseUrl;
	}

	public abstract AnimeParser search(AnilistInfo anilistInfo);

	public abstract AnimeSource getLink(String value);

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
        } else {
            return Integer.parseInt(s);
        }
    }
}
