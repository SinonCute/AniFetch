package live.karyl.anifetch.models;

import live.karyl.anifetch.types.VideoType;

public class VideoResource {
	private int duration;
	private int bandwidth;
	private String url;
	private String backupUrl;
	private String codecs;
	private String sar;
	private String frameRate;
	private String range;
	private String indexRange;
	private int width;
	private int height;
	private String mimeType;
	private String quality;
	private String serverId;
	private String audioQuality;

	private VideoType videoType;

	private boolean useHeader;

	public VideoResource(String url, String quality, String serverId, VideoType videoType) {
		this.url = url;
		this.quality = quality;
		this.serverId = serverId;
		this.videoType = videoType;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getDuration() {
		return duration;
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

	public void setSar(String sar) {
		this.sar = sar;
	}

	public String getSar() {
		return sar;
	}

	public void setFrameRate(String frameRate) {
		this.frameRate = frameRate;
	}

	public String getFrameRate() {
		return frameRate;
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

	public void setWidth(int width) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getHeight() {
		return height;
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

	public VideoType getVideoType() {
		return videoType;
	}

	public void setVideoType(VideoType videoType) {
		this.videoType = videoType;
	}

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public String getAudioQuality() {
		return audioQuality;
	}

	public void setAudioQuality(String audioQuality) {
		this.audioQuality = audioQuality;
	}

	public boolean isUseHeader() {
		return useHeader;
	}

	public void setUseHeader(boolean useHeader) {
		this.useHeader = useHeader;
	}
}
