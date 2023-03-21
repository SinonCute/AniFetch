package live.karyl.anifetch.providers.vn;

import live.karyl.anifetch.models.AnilistInfo;
import live.karyl.anifetch.models.AnimeParser;
import live.karyl.anifetch.providers.AnimeProvider;
import live.karyl.anifetch.utils.SearchRequest;
import live.karyl.anifetch.utils.Utils;
import org.jsoup.nodes.Document;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;

public class WebLinhTinh extends AnimeProvider {

    public WebLinhTinh() {
        super("webLinhTinh", "https://weblinhtinh.net/");
    }

    @Override
    protected AnimeParser search(AnilistInfo anilistInfo) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        AnimeParser animeParser = null;
        List<String> titles = new ArrayList<>();
        titles.add(anilistInfo.getTitle().english);
        titles.add(anilistInfo.getTitle().romaji);
        titles.add(anilistInfo.getTitle().nativeTitle);

        for (String title : titles) {
            if (animeParser != null) break;
            if (title == null) continue;
            var searchResults = SearchRequest.animeTVN(title);
            if (searchResults == null) continue;
            for (var searchResult : searchResults) {
                if (compareResult(anilistInfo, searchResult)) {
                    String id = searchResult.replaceAll("^.*f(\\d+).*$", "$1");
                    //var episodes = extractEpisodeIds(searchResult);
                    animeParser = new AnimeParser(anilistInfo.getId(), id);
                    //animeParser.setEpisodesId(episodes);
                    break;
                }
            }
        }
        stopWatch.stop();
        System.out.println("Search " + anilistInfo.getTitle().english + " in " + siteName + " took " + stopWatch.getTotalTimeSeconds() + "s");
        return animeParser;
    }

    private boolean compareResult(AnilistInfo anilistInfo, String link) {
        Document document = Utils.connect(link);
        String title = document.select(".entry-title").text();
        String year = document.select("title-wrapper").text().replaceAll("\\((\\d+)\\)", "[$1]");
    }
}
