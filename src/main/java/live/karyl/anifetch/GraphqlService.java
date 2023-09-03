package live.karyl.anifetch;

import live.karyl.anifetch.models.AnimeSource;
import live.karyl.anifetch.models.Results;
import live.karyl.anifetch.utils.Utils;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.tinylog.Logger;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Controller
public class GraphqlService {
	@QueryMapping("animeSearch")
	public CompletableFuture<Results> animeSearch(@Argument String id) {
		Logger.debug("Searching for " + id);
		CompletableFuture<Results> future = Utils.searchAllAsyncTimer(id)
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

	@QueryMapping("animeSearchProvider")
	public Results animeSearchProvider(@Argument String id, @Argument String providerId) {
		Logger.debug("Searching for " + id + " on " + providerId);
		var result = Utils.searchProvider(id, providerId);
		if (result.isEmpty()) {
			Logger.debug("No results found on " + id + " on " + providerId);
			return new Results(0, false, null);
		} else {
			Logger.debug("Found " + result.size() + " results on " + id + " on " + providerId);
			return new Results(result.size(), true, result);
		}
	}

	/*@QueryMapping("animeMapping")
	public AnimeMapping animeMapping(@Argument String id, @Argument String providerId) {
		Logger.info("Getting mapping for " + id + " on " + providerId);
		var mediaId = AniFetchApplication.getPostgreSQL().getAnimeFetch(id, providerId);
		if (mediaId == null) {
			Logger.debug("No mapping found for " + id + " on " + providerId + ", searching again");
			Utils.searchProvider(id, providerId);
			mediaId = AniFetchApplication.getPostgreSQL().getAnimeFetch(id, providerId);
		}
		return new AnimeMapping(id, providerId, mediaId);
	}*/

	@QueryMapping("source")
	public AnimeSource source(@Argument String providerId, @Argument String value) {
		Logger.debug("Getting source for " + providerId + " " + value);
		var providers = AniFetchApplication.getProviders();
		var valueDecoded = URLDecoder.decode(value, StandardCharsets.UTF_8);
		for (var provider : providers) {
			if (provider.getSiteId().equals(providerId)) {
				return provider.getLink(valueDecoded, false);
			}
		}
		return null;
	}
}


