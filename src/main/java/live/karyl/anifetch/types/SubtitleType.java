package live.karyl.anifetch.types;

public enum SubtitleType {
	SOFT,
	HARD;

	public static SubtitleType fromString(String type) {
		if (type == null) {
			return null;
		}

		return switch (type) {
			case "soft" -> SOFT;
			case "hard" -> HARD;
			default -> null;
		};
	}
}
