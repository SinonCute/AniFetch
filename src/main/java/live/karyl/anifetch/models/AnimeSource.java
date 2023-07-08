package live.karyl.anifetch.models;

import com.google.gson.Gson;
import live.karyl.anifetch.types.AudioType;
import live.karyl.anifetch.types.SubtitleType;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class AnimeSource {

	private final String providerId;
	private final List<VideoResource> videoResources;
	private final List<AudioResource> audioResources;
	private final List<Subtitle> subtitles;

	private SubtitleType subtitleType;
	private AudioType audioType;

	private final List<Header> headers;

	public AnimeSource(String providerId) {
		this.providerId = providerId;
		videoResources = new ArrayList<>();
		audioResources = new ArrayList<>();
		subtitles = new ArrayList<>();
		headers = new ArrayList<>();
	}

	public String getProviderId() {
		return providerId;
	}

	public void setVideoResources(List<VideoResource> videoResources) {
		this.videoResources.clear();
		this.videoResources.addAll(videoResources);
	}

	public List<VideoResource> getVideoResources() {
		return videoResources;
	}

	public void setAudioResources(List<AudioResource> audioResources) {
		this.audioResources.clear();
		this.audioResources.addAll(audioResources);
	}

	public List<AudioResource> getAudioResources() {
		return audioResources;
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

	public void setHeaders(List<Header> headers) {
		this.headers.clear();
		this.headers.addAll(headers);
	}

	public void addHeader(Header header) {
		for (Header h : headers) {
			if (h.key().equals(header.key())) {
				h.value().addAll(header.value());
				return;
			} else {
				headers.add(header);
			}
		}
	}

	public void addHeader(String key, String value) {
		List<Header> modifiedHeaders = new ArrayList<>();

		if (headers.isEmpty()) {
			modifiedHeaders.add(new Header(key, List.of(value)));
		} else {
			boolean headerUpdated = false;
			for (Header h : headers) {
				if (h.key().equals(key) && !h.value().contains(value)) {
					List<String> values = new ArrayList<>(h.value());
					values.add(value);
					modifiedHeaders.add(new Header(key, values));
					headerUpdated = true;
				} else if (h.key().equals(key) && h.value().contains(value)){
					return;
				} else {
					modifiedHeaders.add(h);
				}
			}
			if (!headerUpdated) {
				modifiedHeaders.add(new Header(key, List.of(value)));
			}
		}

		headers.clear();
		headers.addAll(modifiedHeaders);
	}

	public List<Header> getHeaders() {
		return headers;
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
