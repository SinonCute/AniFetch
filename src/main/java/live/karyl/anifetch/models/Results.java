package live.karyl.anifetch.models;

import java.util.List;

public record Results(Integer totalResults, boolean success, List<AnimeParser> anime) { }
