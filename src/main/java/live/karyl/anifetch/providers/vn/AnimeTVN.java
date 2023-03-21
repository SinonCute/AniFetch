package live.karyl.anifetch.providers.vn;

import live.karyl.anifetch.models.AnilistInfo;
import live.karyl.anifetch.models.AnimeParser;
import live.karyl.anifetch.providers.AnimeProvider;
import live.karyl.anifetch.utils.SearchRequest;
import live.karyl.anifetch.utils.Utils;
import org.jsoup.nodes.Document;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnimeTVN extends AnimeProvider {

	public AnimeTVN() {
		super("AnimeTVN", "https://animetvn.xyz/");
	}

	@Override
	public AnimeParser search(AnilistInfo anilistInfo) {
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
					var episodes = extractEpisodeIds(searchResult);
					animeParser = new AnimeParser(anilistInfo.getId(), id);
					animeParser.setEpisodesId(episodes);
					break;
				}
			}
		}
		stopWatch.stop();
		System.out.println("Search " + anilistInfo.getTitle().english + " in " + siteName + " took " + stopWatch.getTotalTimeSeconds() + "s");
		return animeParser;
	}


	private Map<Integer, String> extractEpisodeIds(String link) {
		var mainPage = Utils.connect(link);
		if (mainPage.select(".play-now").isEmpty()) return null;
		var watchUrl = mainPage.select(".play-now").get(0).attr("href");
		var watchPage = Utils.connect(watchUrl);

		Map<Integer, String> episodes = new HashMap<>();

		int episode = 1;
		for (var server : watchPage.select(".eplist").get(0).getElementsByClass("svep")) {
			if (server.select(".svname").text().equalsIgnoreCase("Trailer")) continue;
			for (var ep : server.select("a.tapphim")) {
				var id = ep.attr("id").split("_")[1];
				episodes.put(episode, id);
				episode++;
			}
		}

		return episodes;
	}

	private boolean compareResult(AnilistInfo anilistInfo, String link) {
		Document document = Utils.connect(link);
		String title = document.select(".name-vi").first().text();
		int year = 0;
		int episode = 0;
		for (var info : document.select(".more-info")) {
			Pattern episodePattern = Pattern.compile("Số tập: </span>(\\d+)");
			Pattern yearPattern = Pattern.compile("Năm phát sóng: </span>(\\w+\\s+\\d{4})");
			Matcher episodeMatcher = episodePattern.matcher(info.html());
			Matcher yearMatcher = yearPattern.matcher(info.html());
			if (episodeMatcher.find()) {
				episode = Integer.parseInt(episodeMatcher.group(1));
			}
			if (yearMatcher.find()) {
				year = Integer.parseInt(yearMatcher.group(1).split(" ")[1]);
			}
		}
		return year == anilistInfo.getReleaseDate()
				&& episode == anilistInfo.getCurrentEpisode()
				&& Utils.matchedRate(title, anilistInfo.getTitle().english) > 0.5;
	}
}
