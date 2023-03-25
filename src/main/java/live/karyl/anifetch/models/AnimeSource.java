package live.karyl.anifetch.models;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class AnimeSource {

	private final String providerId;
	private final List<Source> sources;

	public AnimeSource(String providerId) {
		this.providerId = providerId;
		sources = new ArrayList<>();
	}

	public String getProviderId() {
		return providerId;
	}

	public void addSource(String link, String serverId, String type) {
		sources.add(new Source(link, serverId, type));
	}

	public void addSource(Source source) {
		sources.add(source);
	}

	public List<Source> getSources() { return sources; }

	public String toJson() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	public AnimeSource fromJson(String json) {
		Gson gson = new Gson();
		return gson.fromJson(json, AnimeSource.class);
	}

	public static class Source {
		private final String link;
		private final String type;
		private final String serverId;
		private final List<String> headers;

		public Source(String link, String serverId, String type) {
			this.link = link;
			this.type = type;
			this.serverId = serverId;
			headers = new ArrayList<>();
		}

		public String getLink() {
			return link;
		}

		public String getType() {
			return type;
		}

		public String getServerId() {
			return serverId;
		}

		public void addHeader(String header) {
			headers.add(header);
		}

		public List<String> getHeaders() { return headers; }
	}
}
