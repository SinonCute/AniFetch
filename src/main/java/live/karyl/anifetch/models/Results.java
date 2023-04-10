package live.karyl.anifetch.models;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public record Results(Integer totalResults, boolean success, List<AnimeParser> animes) {}
