package live.karyl.anifetch.providers.eng;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import live.karyl.anifetch.models.*;
import live.karyl.anifetch.providers.AnimeProvider;
import live.karyl.anifetch.types.AudioType;
import live.karyl.anifetch.types.SubtitleType;
import live.karyl.anifetch.types.VideoType;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.tinylog.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Gogoanime extends AnimeProvider {
    public Gogoanime() {
        super("Gogoanime", "gogoanime", "https://api.anify.tv");
    }

    @Override
    public AnimeParser search(AnilistInfo anilistInfo) {
        AnimeParser animeParser;
        String redisId = siteName + "$" + anilistInfo.getId();

        if (redis.exists(redisId, REDIS_NON_EXIST)) {
            return null;
        }

        if (redis.exists(redisId, REDIS_SEARCH)) {
            animeParser = new Gson().fromJson(redis.get(redisId, REDIS_SEARCH), AnimeParser.class);
            if (animeParser != null) return animeParser;
        }

        if (postgreSQL.checkAnimeFetchExists(anilistInfo.getId(), siteName)) {
            var id = postgreSQL.getAnimeFetch(anilistInfo.getId(), siteName);
            var episodes = extractEpisodeIds(anilistInfo.getId());
            animeParser = new AnimeParser(anilistInfo.getId(), id, siteId, siteName);
            animeParser.setEpisodes(episodes);
            redis.set(redisId, animeParser.toJson(), REDIS_SEARCH);
            return animeParser;
        }

        var episodes = extractEpisodeIds(anilistInfo.getId());
        animeParser = new AnimeParser(anilistInfo.getId(), anilistInfo.getId(), siteId, siteName);
        animeParser.setEpisodes(episodes);
        redis.set(redisId, animeParser.toJson(), REDIS_SEARCH);
        postgreSQL.addAnimeFetch(animeParser);

        return animeParser;
    }

    @Override
    public AnimeSource getLink(String value, boolean ignoreCache) {
        AnimeSource animeSource = new AnimeSource(siteName);
        String redisId = siteName + "$" + value;

        var episodeId = value.split("\\$")[0];
        var episodeNumber = value.split("\\$")[1];
        var animeId = value.split("\\$")[2];

        if (redis.exists(redisId, REDIS_SOURCE) && !ignoreCache) {
            String jsonData = redis.get(redisId, REDIS_SOURCE);
            return new Gson().fromJson(jsonData, AnimeSource.class);
        }

        RequestBody requestBody = new FormBody.Builder()
                .addEncoded("providerId", siteId)
                .addEncoded("watchId", episodeId)
                .addEncoded("episode", episodeNumber)
                .addEncoded("id", animeId)
                .build();
        var request = new Request.Builder()
                .url(baseUrl + "/sources?apikey=" + getAnifyApiKey())
                .post(requestBody)
                .build();
        var response = connection.callWithoutRateLimit(request);
        try {
            var object = new Gson().fromJson(response.body().string(), JsonObject.class);
            var sourcesArray = object.get("sources").getAsJsonArray();
            for (var source : sourcesArray) {
                var sourceObject = source.getAsJsonObject();
                var sourceUrl = sourceObject.get("url").getAsString();
                var sourceQuality = sourceObject.get("quality").getAsString();
                animeSource.addVideoResource(new VideoResource(sourceUrl, sourceQuality, "default", VideoType.HLS));
            }
            if (object.get("subtitles").getAsJsonArray().isEmpty()) {
                animeSource.setSubtitleType(SubtitleType.HARD);
            } else {
                animeSource.setSubtitleType(SubtitleType.SOFT);
            }
            animeSource.setAudioType(AudioType.HARD);
            redis.set(redisId, animeSource.toJson(), REDIS_SOURCE);
            return animeSource;
        } catch (Exception e) {
            Logger.error("[%s] Error while parsing sources: ".formatted(siteId) + e.getMessage());
        }
        return null;
    }

    private List<AnimeEpisode> extractEpisodeIds (String animeId) {
        var request = new Request.Builder()
                .url(baseUrl + "/episodes/%s/?apikey=%s".formatted(animeId, getAnifyApiKey()))
                .build();
        var response = connection.callWithoutRateLimit(request);
        try {
            var json = new Gson().fromJson(response.body().string(), JsonElement.class);
            var jsonArray = json.getAsJsonArray();
            var episodes = new ArrayList<AnimeEpisode>();
            for (var jsonElement : jsonArray) {
                var jsonObject = jsonElement.getAsJsonObject();
                if (!jsonObject.get("providerId").getAsString().equals(siteId)) continue;
                var episodeArray = jsonObject.get("episodes").getAsJsonArray();
                for (var episode : episodeArray) {
                    var episodeObject = episode.getAsJsonObject();
                    var episodeId = episodeObject.get("id").getAsString();
                    var episodeNumber = episodeObject.get("number").getAsInt();
                    var episodeTitle = episodeObject.get("title").getAsString();
                    var customEpisodeId = episodeId + "$" + episodeNumber + "$" + animeId; //episodeId$episodeNumber$animeId
                    episodes.add(new AnimeEpisode(episodeTitle, episodeNumber, customEpisodeId));
                }
                break;
            }
            episodes.sort(Comparator.comparingInt(AnimeEpisode::episodeNumber));
            return episodes;
        } catch (Exception e) {
            Logger.error("[%s] Error while parsing episodes: ".formatted(siteId) + e.getMessage());
        }
        return null;
    }
}
