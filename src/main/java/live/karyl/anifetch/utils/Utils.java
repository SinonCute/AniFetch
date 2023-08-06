package live.karyl.anifetch.utils;

import live.karyl.anifetch.AniFetchApplication;
import live.karyl.anifetch.config.ConfigManager;
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Utils {

    private static final ConfigManager config = AniFetchApplication.getConfig();

    public static AnilistInfo fetchAnilist(String id) {
        try {
            String url = config.getAniListUrl() + "/meta/anilist/info/" + id;
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Karyl/1.0.0")
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
        int retry = config.getOkHttpRetry();
        int timeout = 5000;
        Document document = null;
        while (retry > 0) {
            try {
                document = Jsoup.connect(url)
                        .userAgent(config.getUserAgent())
                        .header("Cookie", cookie)
                        .timeout(timeout)
                        .get();
                break;
            } catch (Exception e) {
                e.printStackTrace();
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

    public static CompletableFuture<List<AnimeParser>> searchAllAsync(String id) {
        AnilistInfo anilistInfo = fetchAnilist(id);
        List<CompletableFuture<AnimeParser>> futures = new ArrayList<>();

        for (var provider : AniFetchApplication.getProviders()) {
            CompletableFuture<AnimeParser> future = CompletableFuture.supplyAsync(() -> provider.search(anilistInfo))
                    .thenApply(animeParser -> {
                        if (animeParser == null || animeParser.getEpisodes() == null || animeParser.getEpisodes().isEmpty()) {
                            return null;
                        }
                        return animeParser;
                    });
            if (future == null) continue;
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

    public static List<AnimeParser> searchProvider(String id, String providerId) {
        AnilistInfo anilistInfo = fetchAnilist(id);
        for (var provider : AniFetchApplication.getProviders()) {
            if (provider.getSiteId().equals(providerId)) {
                AnimeParser result = provider.search(anilistInfo);
                if (result == null || result.getEpisodes() == null || result.getEpisodes().isEmpty()) {
                    return List.of();
                } else {
                    return List.of(result);
                }
            }
        }
        return List.of();
    }

    public static CompletableFuture<List<AnimeParser>> searchAllAsyncTimer(String id) {
        AnilistInfo anilistInfo = fetchAnilist(id);
        List<CompletableFuture<AnimeParser>> futures = new ArrayList<>();

        for (var provider : AniFetchApplication.getProviders()) {
            CompletableFuture<AnimeParser> future = CompletableFuture.supplyAsync(() -> {
                // Start stopwatch
                long startTime = System.nanoTime();

                AnimeParser result = provider.search(anilistInfo);

                // Stop stopwatch and calculate duration
                long endTime = System.nanoTime();
                long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

                // Print the duration for the provider's search operation
                Logger.debug("Provider: " + provider.getSiteName()+ ", Duration: " + duration + "ms");

                return result;
            }).thenApply(animeParser -> {
                if (animeParser == null || animeParser.getEpisodes() == null || animeParser.getEpisodes().isEmpty()) {
                    return null;
                }
                return animeParser;
            });

            if (future == null) continue;
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }


    public static boolean checkNumberEqual(int int1, int int2) {
        return Math.abs(int1 - int2) <= 1;
    }

    public static boolean checkURLBilibili(String url) {
        Request request;
        if (url.contains("-bstar1-")) {
            request = new Request.Builder()
                    .url(url)
                    .build();
        } else {
            request = new Request.Builder()
                    .url(url)
                    .header("Referer", "https://www.bilibili.tv/")
                    .build();
        }
        Response response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
        if (response.code() != 200) {
            response.close();
            return false;
        }
        response.close();
        return true;

    }
}
