package live.karyl.anifetch.types;

public enum VideoType {
	MP4,
	HLS,
    DASH;

	public static VideoType fromString(String type) {
		if (type == null) {
			return null;
		}

		return switch (type) {
			case "mp4" -> MP4;
			case "hls" -> HLS;
			case "dash" -> DASH;
			default -> null;
		};
	}

	public String toString() {
		return switch (this) {
			case MP4 -> "MP4";
			case HLS -> "HLS";
			case DASH -> "DASH";
		};
	}
}
