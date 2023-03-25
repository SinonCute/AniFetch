package live.karyl.anifetch.models;

public class AnimeSource {
	private final String providerId;
	private final String serverId;
	private final String link;

	public AnimeSource(String link, String providerId, String serverId) {
		this.link = link;
		this.providerId = providerId;
		this.serverId = serverId;
	}

	public String getLink() {
		return link;
	}

	public String getProviderId() { return providerId; }

	public String getServerId() { return serverId; }
}
