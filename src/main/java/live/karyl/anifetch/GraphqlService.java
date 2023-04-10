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
		var providers = AniFetchApplication.getProviders().values();
		for (var provider : providers) {
			if (provider.getSiteId().equals(providerId)) {
				return provider.getLink(value);
			}
		}
		return null;
	}
}


