package live.karyl.anifetch.types;

public enum AudioType {
	SOFT,
	HARD;

	public static AudioType fromString(String type) {
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
