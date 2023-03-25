package live.karyl.anifetch;

import live.karyl.anifetch.models.AnimeParser;
import live.karyl.anifetch.models.AnimeSource;
import live.karyl.anifetch.utils.Utils;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class GraphqlService {

	@QueryMapping("anime")
	public List<AnimeParser> episode(@Argument String id) {
		System.out.println("Searching for " + id);
		return Utils.searchAll(id);
	}

	@QueryMapping("source")
	public AnimeSource source(@Argument String providerId, @Argument String value) {
		System.out.println("Searching for " + value);
		switch (providerId) {
			case "AnimeTVN" -> {
				return AniFetchApplication.getProviders().get("AnimeTVN").getLink(value);
			}
			case "WebLinhTinh" -> {
				return AniFetchApplication.getProviders().get("WebLinhTinh").getLink(value);
			}
		}
		return null;
	}
}


