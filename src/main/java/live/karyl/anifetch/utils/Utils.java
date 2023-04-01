package live.karyl.anifetch.utils;

import live.karyl.anifetch.AniFetchApplication;
import live.karyl.anifetch.models.AnilistInfo;
import live.karyl.anifetch.models.AnimeParser;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Utils {

    public static AnilistInfo fetchAnilist(String id) {
        try {
            String url = "https://api.karyl.live/consumet/meta/anilist/info/" + id;
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
            String result = response.body().string();
            if (response.code() != 200) {
                response.close();
                return null;
            }
            response.close();
            return AnilistInfo.fromJson(result);
        } catch (Exception e) {
            Logger.error(e);
            return null;
        }
    }

    public static Document connect(String url, String cookie) {
        int retry = 5;
        int timeout = 5000;
        Document document = null;
        while (retry > 0) {
            try {
                document = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36")
                        .header("Cookie", cookie)
                        .timeout(timeout)
                        .get();
                break;
            } catch (Exception e) {
                Logger.debug(retry + " Retry connect to " + url);
                retry--;
            }
        }
        return document;
    }

    public static double matchedRate(String title1, String title2) {
        JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();
        return jaroWinklerSimilarity.apply(title1.toLowerCase(Locale.ROOT), title2.toLowerCase(Locale.ROOT));
    }

    public static List<AnimeParser> searchAll(String id) {
        AnilistInfo anilistInfo = fetchAnilist(id);
        List<AnimeParser> animeParsers = new ArrayList<>();
        for (var provider : AniFetchApplication.getProviders().values()) {
            var animeParser = provider.search(anilistInfo);
            if (animeParser == null) continue;
            if (animeParser.getEpisodes() == null) continue;
            animeParsers.add(animeParser);
        }
        return animeParsers;
    }

    public static boolean checkNumberEqual(int int1, int int2) {
        return Math.abs(int1 - int2) <= 1;
    }
}
