package live.karyl.anifetch.models;

public class AudioResource {
	private int bandwidth;
	private String codecs;
	private String range;
	private String indexRange;
	private String mimeType;
	private String url;
	private String backupUrl;
	private String quality;

	public AudioResource(String url, String quality) {
		this.url = url;
		this.quality = quality;
	}

	public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}

	public int getBandwidth() {
		return bandwidth;
	}

	public void setCodecs(String codecs) {
		this.codecs = codecs;
	}

	public String getCodecs() {
		return codecs;
	}

	public void setRange(String range) {
		this.range = range;
	}

	public String getRange() {
		return range;
	}

	public void setIndexRange(String indexRange) {
		this.indexRange = indexRange;
	}

	public String getIndexRange() {
		return indexRange;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getMimeType() {
		return mimeType;
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
