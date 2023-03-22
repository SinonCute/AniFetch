package live.karyl.anifetch.models;

import com.google.gson.Gson;

import java.util.*;

public class AnimeParser {
	private final String animeId;
	private final String providerId;
	private final String providerName;

	private final List<Episode> episodes;

	public AnimeParser(String animeId, String providerId, String providerName) {
		this.animeId = animeId;
		this.providerId = providerId;
		this.providerName = providerName;
		episodes = new ArrayList<>();
	}

	public String getAnimeId() {
		return animeId;
	}

	public String getProviderId() {
		return providerId;
	}

	public List<Episode> getEpisodes() {
		return episodes;
	}

	public String getProviderName() { return providerName; }

	public void setEpisodes(List<String> episodesId) {
		episodes.clear();
		for (int i = 0; i < episodesId.size(); i++) {
			episodes.add(new Episode(i + 1, episodesId.get(i)));
		}
	}

	public String toJson() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	public AnimeParser fromJson(String json) {
		Gson gson = new Gson();
		return gson.fromJson(json, AnimeParser.class);
	}

	public record Episode(int episodeNumber, String value) {}
}
