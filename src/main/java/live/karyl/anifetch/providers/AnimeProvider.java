package live.karyl.anifetch.providers;

import live.karyl.anifetch.AniFetchApplication;
import live.karyl.anifetch.config.ConfigManager;
import live.karyl.anifetch.connection.OkHttp;
import live.karyl.anifetch.database.MongoDB;
import live.karyl.anifetch.database.Redis;
import live.karyl.anifetch.models.AnilistInfo;
import live.karyl.anifetch.models.AnimeMapping;
import live.karyl.anifetch.models.AnimeParser;
import live.karyl.anifetch.models.AnimeSource;
import live.karyl.anifetch.utils.Utils;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.time.StopWatch;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public abstract class AnimeProvider {
    protected final String siteName;
    protected final String siteId;
    protected final String baseUrl;

    protected String USER_AGENT = "";
    protected String PROXY_VN = "";

    protected final String REDIS_SEARCH = "search";
    protected final String REDIS_SOURCE = "source";
    protected final String REDIS_NON_EXIST = "non_exist";

    protected final String ANIFY_API_KEY = "67a51de430d3fd64a9b5f6b8a77a3558";


    protected final OkHttp connection = AniFetchApplication.getConnection();
    protected final Redis redis = AniFetchApplication.getRedis();
    protected final MongoDB mongoDB = AniFetchApplication.getMongoDB();
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
        StopWatch stopWatch = StopWatch.createStarted();
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
                    stopWatch.stop();
                    System.out.println("Time to connect: " + stopWatch.getTime());
                    return Jsoup.parse(new String(response.body().bytes(), StandardCharsets.UTF_8));
                } catch (Exception e) {
                    stopWatch.stop();
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
                    stopWatch.stop();
                    System.out.println("Time to connect: " + stopWatch.getTime());
                    return Jsoup.parse(new String(response.body().bytes(), StandardCharsets.UTF_8));
                } catch (Exception e) {
                    stopWatch.stop();
                    e.printStackTrace();
                    return null;
                }
            }
            default -> {
                stopWatch.stop();
                System.out.println("Time to connect: " + stopWatch.getTime());
                return Utils.connect(url, "");
            }
        }
    }

    protected static int extractNumberFromString(String s) {
        try {
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
        } catch (Exception e) {
            return -1;
        }
    }

    protected void addAnimeMapping(String animeId, String providerId, String mediaId) {
        AnimeMapping dbMapping = AniFetchApplication.getMongoDB().getAnimeMapping(animeId);
        if (dbMapping != null) {
            if (dbMapping.mappings().containsKey(providerId)) {
                return;
            }
            dbMapping.mappings().put(providerId, mediaId);
            AniFetchApplication.getMongoDB().updateAnimeMapping(dbMapping);
        } else {
            AnimeMapping animeMapping = new AnimeMapping(animeId, Map.of(providerId, mediaId));
            AniFetchApplication.getMongoDB().addAnimeMapping(animeMapping);
        }
    }

    public String getSiteName() {
        return siteName;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getAnifyApiKey() { return  ANIFY_API_KEY; }
}
