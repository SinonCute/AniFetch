package live.karyl.anifetch.models;

import com.google.gson.Gson;

import java.util.*;

public class AnimeParser {
	private final String animeId;
	private final String mediaId;
	private final String providerId;
	private final String providerName;

	private List<AnimeEpisode> episodes;

	public AnimeParser(String animeId, String mediaId, String providerId, String providerName) {
		this.animeId = animeId;
		this.mediaId = mediaId;
		this.providerId = providerId;
		this.providerName = providerName;
		episodes = new ArrayList<>();
	}

	public String getAnimeId() {
		return animeId;
	}

	public String getMediaId() { return mediaId; }

	public String getProviderId() {
		return providerId;
	}

	public List<AnimeEpisode> getEpisodes() {
		return episodes;
	}

	public String getProviderName() { return providerName; }

	public void setEpisodes(List<AnimeEpisode> episodes) {
		this.episodes = episodes;
	}

	public String toJson() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	public AnimeParser fromJson(String json) {
		Gson gson = new Gson();
		return gson.fromJson(json, AnimeParser.class);
	}
}
