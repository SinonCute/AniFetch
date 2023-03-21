package live.karyl.anifetch.providers;

import live.karyl.anifetch.AniFetchApplication;
import live.karyl.anifetch.connection.OkHttp;
import live.karyl.anifetch.models.AnilistInfo;
import live.karyl.anifetch.models.AnimeParser;

public abstract class AnimeProvider {
	protected final String siteName;
	protected final String baseUrl;

	protected final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.134 Safari/537.36";

	protected final OkHttp connection = AniFetchApplication.getConnection();

	public AnimeProvider(String siteName, String baseUrl) {
		this.siteName = siteName;
		this.baseUrl = baseUrl;
	}

	protected abstract AnimeParser search(AnilistInfo anilistInfo);
}
