package live.karyl.anifetch.models;

public class AudioResource {
	private String url;
	private String backupUrl;
	private String quality;

	public AudioResource(String url, String quality) {
		this.url = url;
		this.quality = quality;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getBackupUrl() {
		return backupUrl;
	}

	public void setBackupUrl(String backupUrl) {
		this.backupUrl = backupUrl;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}
}
