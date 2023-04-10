package live.karyl.anifetch.providers.vn;

import com.google.gson.Gson;
import live.karyl.anifetch.models.*;
import live.karyl.anifetch.providers.AnimeProvider;
import live.karyl.anifetch.types.AudioType;
import live.karyl.anifetch.types.SubtitleType;
import live.karyl.anifetch.types.VideoType;
import live.karyl.anifetch.utils.SearchRequest;
import live.karyl.anifetch.utils.Utils;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebLinhTinh extends AnimeProvider {

    public WebLinhTinh() {
        super("webLinhTinh", "WLT","https://weblinhtinh.net/");
    }

    @Override
    public AnimeParser search(AnilistInfo anilistInfo) {
        AnimeParser animeParser = null;
        String redisId = siteName + "$" + anilistInfo.getId();
        Map<String, String> titles = new HashMap<>();
        titles.put("english", anilistInfo.getTitle().english);
        titles.put("romaji", anilistInfo.getTitle().romaji);

        if (redis.exists(redisId, REDIS_SEARCH)) {
            animeParser = new Gson().fromJson(redis.get(redisId, REDIS_SEARCH), AnimeParser.class);
            if (animeParser != null) return animeParser;
        }

        if (postgreSQL.checkAnimeFetchExists(anilistInfo.getId(), siteName)) {
            var id = postgreSQL.getAnimeFetch(anilistInfo.getId(), siteName);
            var episodes = extractEpisodeIds(id);
            animeParser = new AnimeParser(anilistInfo.getId(), id, siteId, siteName);
            animeParser.setEpisodes(episodes);
            redis.set(redisId, animeParser.toJson(), REDIS_SEARCH);
            return animeParser;
        }

        for (var key : titles.keySet()) {
            var title = titles.get(key);
            if (animeParser != null) break;
            var searchResults = SearchRequest.webLinhTinh(title);
            if (searchResults == null) continue;
            for (var searchResult : searchResults) {
                var mainPage = connect(searchResult, siteName);
                if (compareResult(mainPage, anilistInfo, key)) {
                    var id = mainPage.select("#bookmark").attr("data-id");
                    animeParser = new AnimeParser(anilistInfo.getId(), id, siteId, siteName);
                    animeParser.setEpisodes(extractEpisodeIds(id));
                    postgreSQL.addAnimeFetch(animeParser);
                    break;
                }
            }
        }
        if (animeParser == null) return null;
        redis.set(redisId, animeParser.toJson(), REDIS_SEARCH);
        return animeParser;
    }

    public List<AnimeEpisode> extractEpisodeIds(String id) {
        List<AnimeEpisode> episodes = new ArrayList<>();
        var response = requestPostGetLink(new String[]{"", "", id}, "getEpisode");
        if (response == null) return episodes;
        var document = Jsoup.parse(response);
        for (var a : document.select("span.halim-btn")) {
            String episode = a.attr("data-episode");
            String server = a.attr("data-server");
            String postid = a.attr("data-post-id");
            episodes.add(new AnimeEpisode(episode, Integer.parseInt(episode), episode + "$" + server + "$" + postid));
        }
        return episodes;
    }

    @Override
    public AnimeSource getLink(String data) {
        AnimeSource animeSource = new AnimeSource(siteName);
        String redisId = siteName + "$" + data;


        if (redis.exists(redisId, REDIS_SOURCE)) {
            String jsonData = redis.get(redisId, REDIS_SOURCE);
            return new Gson().fromJson(jsonData, AnimeSource.class);
        }

        var value = data.split("\\$");
        var response = requestPostGetLink(new String[]{value[0], value[1], value[2]}, "getLink");
        String pattern = "sources:\\s*(\\[.+?\\])";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(response);

        if (matcher.find()) {
            String fileValue = matcher.group(1);
            Gson gson = new Gson();
            var jsonObject = gson.fromJson(fileValue, Object.class);
            var sourcesArray = gson.toJsonTree(jsonObject).getAsJsonArray();

            for (var a : sourcesArray) {
                var sourceObject = a.getAsJsonObject();
                String link = sourceObject.get("file").getAsString();
                var videoResource = new VideoResource(link, "720P", value[1], VideoType.HLS);
                animeSource.addVideoResource(videoResource);
            }
        }
        animeSource.setSubtitleType(SubtitleType.HARD);
        animeSource.setAudioType(AudioType.HARD);

        redis.set(redisId, animeSource.toJson(), REDIS_SOURCE);
        return animeSource;
    }

    private boolean compareResult(Document mainPage, AnilistInfo anilistInfo, String type) {
        try {
            var title = mainPage.select(".title-wrapper > .entry-title").text();
            int episode = mainPage.select(".halim-list-eps > li").size();
            if (type.equals("english")) {
                return  Utils.checkNumberEqual(episode, anilistInfo.getCurrentEpisode())
                        && Utils.matchedRate(title, anilistInfo.getTitle().english) > 0.5;
            }
            return Utils.checkNumberEqual(episode, anilistInfo.getCurrentEpisode())
                    && Utils.matchedRate(title, anilistInfo.getTitle().romaji) > 0.5;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String requestPostGetLink(String[] data, String actionType) {
        try {
            String url = "https://weblinhtinh.net/wp-admin/admin-ajax.php";
            FormBody req;
            String action = "";
            switch (actionType) {
                case "getEpisode" -> action = "halim_ajax_show_all_eps_list";
                case "getLink" -> action = "halim_ajax_player";
            }

            req = new FormBody.Builder()
                    .addEncoded("action", action)
                    .addEncoded("episode", data[0])
                    .addEncoded("server", data[1])
                    .addEncoded("postid", data[2])
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(req)
                    .build();
            Response response = connection.callWithoutRateLimit(request);
            if (response.code() != 200 || response.body() == null) {
                return null;
            }
            return response.body().string();
        } catch (Exception e) {
            return null;
        }
    }
}
