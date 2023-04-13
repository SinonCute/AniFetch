package live.karyl.anifetch;

import live.karyl.anifetch.models.AnimeSource;
import live.karyl.anifetch.models.Results;
import live.karyl.anifetch.utils.Utils;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Controller
public class GraphqlService {

	@QueryMapping("animeSearch")
	public CompletableFuture<Results> animeSearch(@Argument String id) {
		System.out.println("Searching for " + id);
		CompletableFuture<Results> future = Utils.searchAllAsync(id)
				.thenApplyAsync(animeParsers -> {
					if (animeParsers.isEmpty()) {
						System.out.println("No results found");
						return new Results(0, false, null);
					} else {
						System.out.println("Found " + animeParsers.size() + " results");
						return new Results(animeParsers.size(), true, animeParsers);
					}
				});
		return future.orTimeout(25, TimeUnit.SECONDS).exceptionally(e -> {
			System.out.println("Error: " + e.getMessage());
			return new Results(0, false, null);
		});
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


