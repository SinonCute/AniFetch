package live.karyl.anifetch.providers.vn;

import live.karyl.anifetch.models.AnilistInfo;
import live.karyl.anifetch.models.AnimeParser;
import live.karyl.anifetch.providers.AnimeProvider;

public class AnimeTVN extends AnimeProvider {

	public AnimeTVN() {
		super("AnimeTVN", "https://animetvn.xyz/");
	}


	@Override
	protected AnimeParser search(AnilistInfo anilistInfo) {
		AnimeParser animeParser = new AnimeParser(null, null);
		var title = anilistInfo.getTitle().english;
		var releaseDate = anilistInfo.getReleaseDate();



		return animeParser;
	}
}
