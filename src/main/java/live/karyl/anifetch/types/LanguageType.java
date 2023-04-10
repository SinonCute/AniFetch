package live.karyl.anifetch.types;

public enum LanguageType {
	EN,
	VI;

	public static LanguageType fromString(String type) {
		if (type == null) {
			return null;
		}

		return switch (type) {
			case "en", "English" -> EN;
			case "vi", "Tiếng việt" -> VI;
			default -> null;
		};
	}

	public String toString() {
		return switch (this) {
			case EN -> "English";
			case VI -> "Tiếng việt";
		};
	}
}
