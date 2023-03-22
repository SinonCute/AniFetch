package live.karyl.anifetch.providers.vn;

import com.google.gson.Gson;
import live.karyl.anifetch.models.AnilistInfo;
import live.karyl.anifetch.models.AnimeParser;
import live.karyl.anifetch.models.AnimeSource;
import live.karyl.anifetch.providers.AnimeProvider;
import live.karyl.anifetch.utils.SearchRequest;
import live.karyl.anifetch.utils.Utils;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebLinhTinh extends AnimeProvider {

    public WebLinhTinh() {
        super("webLinhTinh", "https://weblinhtinh.net/");
    }

    @Override
    public AnimeParser search(AnilistInfo anilistInfo) {
        AnimeParser animeParser = null;
        String redisId = siteName + "$" + anilistInfo.getId();
        List<String> titles = new ArrayList<>();
        titles.add(anilistInfo.getTitle().english);
        titles.add(anilistInfo.getTitle().romaji);
        titles.add(anilistInfo.getTitle().nativeTitle);

        if (redis.exists(redisId, "search")) {
            animeParser = new Gson().fromJson(redis.get(redisId, "search"), AnimeParser.class);
            if (animeParser != null) return animeParser;
        }

        if (redis.exists(redisId, "provider")) {
            String id = redis.get(redisId, "provider");
            var episodes = extractEpisodeIds(id);
            animeParser = new AnimeParser(anilistInfo.getId(), id, siteName);
            animeParser.setEpisodes(episodes);
            redis.set(redisId, animeParser.toJson(), "search");
            return animeParser;
        }

        for (String title : titles) {
            if (animeParser != null) break;
            var searchResults = SearchRequest.webLinhTinh(title);
            if (searchResults == null) continue;
            for (var searchResult : searchResults) {
                var mainPage = Utils.connect(searchResult);
                if (compareResult(mainPage, anilistInfo)) {
                    var id = mainPage.select("#bookmark").attr("data-id");
                    animeParser = new AnimeParser(anilistInfo.getId(), id, siteName);
                    animeParser.setEpisodes(extractEpisodeIds(id));
                    redis.set(redisId, id, "provider");
                    break;
                }
            }
        }
        redis.set(redisId, animeParser.toJson(), "search");
        return animeParser;
    }

    public List<String> extractEpisodeIds(String id) {
        List<String> episodes = new ArrayList<>();
        var response = requestPostGetLink(new String[]{"", "", id}, "getEpisode");
        if (response == null) return episodes;
        var document = Jsoup.parse(response);
        for (var a : document.select("span.halim-btn")) {
            String episode = a.attr("data-episode");
            String server = a.attr("data-server");
            String postid = a.attr("data-post-id");
            episodes.add(episode + "$" + server + "$" + postid);
        }
        return episodes;
    }

    @Override
    public AnimeSource getLink(String data) {
        String redisId = siteName + "$" + data;
        String link = "";

        if (redis.exists(redisId, "source")) {
            link = redis.get(redisId, "source");
            if (link != null) return new AnimeSource(link);
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
                link = sourceObject.get("file").getAsString();
                redis.set(redisId, link, "source");
            }
        }
        return new AnimeSource(link);
    }

    private boolean compareResult(Document mainPage, AnilistInfo anilistInfo) {
        var title = mainPage.select(".title-wrapper > .entry-title").text();
        int year = Integer.parseInt(StringUtils.substringBetween(mainPage.select(".title-wrapper").html(), "(", ")"));
        int episode = Integer.parseInt(mainPage.select(".more-info").get(0).text().split("/")[0]);
        return year == anilistInfo.getReleaseDate()
                && episode == anilistInfo.getCurrentEpisode()
                && Utils.matchedRate(title, anilistInfo.getTitle().english) > 0.5;
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
