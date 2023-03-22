package live.karyl.anifetch.providers;

import live.karyl.anifetch.AniFetchApplication;
import live.karyl.anifetch.connection.OkHttp;
import live.karyl.anifetch.database.Redis;
import live.karyl.anifetch.models.AnilistInfo;
import live.karyl.anifetch.models.AnimeParser;
import live.karyl.anifetch.models.AnimeSource;

public abstract class AnimeProvider {
	protected final String siteName;
	protected final String baseUrl;

	protected final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.134 Safari/537.36";

	protected final OkHttp connection = AniFetchApplication.getConnection();
	protected final Redis redis = AniFetchApplication.getRedis();

	public AnimeProvider(String siteName, String baseUrl) {
		this.siteName = siteName;
		this.baseUrl = baseUrl;
	}

	public abstract AnimeParser search(AnilistInfo anilistInfo);

	public abstract AnimeSource getLink(String value);
}
