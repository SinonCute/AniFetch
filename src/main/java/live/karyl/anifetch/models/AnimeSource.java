package live.karyl.anifetch.models;

import com.google.gson.Gson;
import live.karyl.anifetch.types.AudioType;
import live.karyl.anifetch.types.SubtitleType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class AnimeSource {

	private final String providerId;
	private final List<VideoResource> videoResources;
	private final List<AudioResource> audioResources;
	private final List<Subtitle> subtitles;

	private SubtitleType subtitleType;
	private AudioType audioType;

	private final Map<String, String[]> headers;

	public AnimeSource(String providerId) {
		this.providerId = providerId;
		videoResources = new ArrayList<>();
		audioResources = new ArrayList<>();
		subtitles = new ArrayList<>();
		headers = new HashMap<>();
	}

	public String getProviderId() {
		return providerId;
	}

	public void setVideoResources(List<VideoResource> videoResources) {
		this.videoResources.clear();
		this.videoResources.addAll(videoResources);
	}

	public void setAudioResources(List<AudioResource> audioResources) {
		this.audioResources.clear();
		this.audioResources.addAll(audioResources);
	}

	public void setSubtitles(List<Subtitle> subtitles) {
		this.subtitles.clear();
		this.subtitles.addAll(subtitles);
	}

	public void setSubtitleType(SubtitleType subtitleType) {
		this.subtitleType = subtitleType;
	}

	public void setAudioType(AudioType audioType) {
		this.audioType = audioType;
	}

	public void setHeaders(Map<String, String[]> headers) {
		this.headers.clear();
		this.headers.putAll(headers);
	}

	public void addHeader(String key, String[] value) {
		headers.put(key, value);
	}

	public void addVideoResource(VideoResource videoResource) {
		videoResources.add(videoResource);
	}

	public void addAudioResource(AudioResource audioResource) {
		audioResources.add(audioResource);
	}

	public void addSubtitle(Subtitle subtitle) {
		subtitles.add(subtitle);
	}

	public String toJson() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	public AnimeSource fromJson(String json) {
		Gson gson = new Gson();
		return gson.fromJson(json, AnimeSource.class);
	}
}
