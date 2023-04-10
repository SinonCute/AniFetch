package live.karyl.anifetch;

import live.karyl.anifetch.models.AnimeParser;
import live.karyl.anifetch.models.AnimeSource;
import live.karyl.anifetch.models.Results;
import live.karyl.anifetch.utils.Utils;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class GraphqlService {

	@QueryMapping("anime")
	public Results episode(@Argument String id) {
		System.out.println("Searching for " + id);
		List<AnimeParser> animeParsers = Utils.searchAll(id);
		if (animeParsers.isEmpty()) {
			return new Results(0, false, null);
		}
		return new Results(animeParsers.size(), true, animeParsers);
	}

	@QueryMapping("source")
	public AnimeSource source(@Argument String providerId, @Argument String value) {
		System.out.println("Getting source for " + providerId + " " + value);
		switch (providerId) {
			case "ATVN" -> {
				return AniFetchApplication.getProviders().get("AnimeTVN").getLink(value);
			}
			case "WLT" -> {
				return AniFetchApplication.getProviders().get("WebLinhTinh").getLink(value);
			}
			case "AH" -> {
				return AniFetchApplication.getProviders().get("AnimeHay").getLink(value);
			}
			case "AV" -> {
				return AniFetchApplication.getProviders().get("AnimeVietsub").getLink(value);
			}
			case "BL" -> {
				return AniFetchApplication.getProviders().get("Bilibili").getLink(value);
			}
			default -> {
				return null;
			}
		}
	}
}


