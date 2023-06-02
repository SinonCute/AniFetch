package live.karyl.anifetch;

import live.karyl.anifetch.models.AnimeSource;
import live.karyl.anifetch.models.Results;
import live.karyl.anifetch.utils.Utils;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.util.StopWatch;
import org.tinylog.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Controller
public class GraphqlService {

	@QueryMapping("animeSearch")
	public CompletableFuture<Results> animeSearch(@Argument String id) {
		Logger.debug("Searching for " + id);
		CompletableFuture<Results> future = Utils.searchAllAsync(id)
				.thenApplyAsync(animeParsers -> {
					if (animeParsers.isEmpty()) {
						Logger.debug("No results found on" + id);
						return new Results(0, false, null);
					} else {
						Logger.debug("Found " + animeParsers.size() + " results on " + id);
						return new Results(animeParsers.size(), true, animeParsers);
					}
				});
		return future.orTimeout(30, TimeUnit.SECONDS).exceptionally(e -> {
			Logger.error(e);
			Logger.debug("Timeout on " + id);
			return new Results(0, false, null);
		});
	}

	@QueryMapping("source")
	public AnimeSource source(@Argument String providerId, @Argument String value) {
		Logger.debug("Getting source for " + providerId + " " + value);
		var providers = AniFetchApplication.getProviders().values();
		for (var provider : providers) {
			if (provider.getSiteId().equals(providerId)) {
				return provider.getLink(value, false);
			}
		}
		return null;
	}
}


