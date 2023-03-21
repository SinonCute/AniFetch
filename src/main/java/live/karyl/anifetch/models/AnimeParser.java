package live.karyl.anifetch.models;

import java.util.HashMap;
import java.util.Map;

public class AnimeParser {
	private final String animeId;
	private final String providerId;

	private Map<Integer, String> episodesId;

	public AnimeParser(String animeId, String providerId) {
		this.animeId = animeId;
		this.providerId = providerId;
		episodesId = new HashMap<>();
	}

	public String getAnimeId() {
		return animeId;
	}

	public String getProviderId() {
		return providerId;
	}

	public Map<Integer, String> getEpisodesId() {
		return episodesId;
	}

	public void setEpisodesId(Map<Integer, String> episodesId) {
		this.episodesId = episodesId;
	}

	public void addEpisodeId(int episode, String id) {
		episodesId.put(episode, id);
	}
}
