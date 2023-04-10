package live.karyl.anifetch.models;

import live.karyl.anifetch.types.VideoType;

public class VideoResource {
	private String url;
	private String backupUrl;
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
