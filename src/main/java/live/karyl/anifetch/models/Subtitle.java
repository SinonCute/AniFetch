package live.karyl.anifetch.models;

import live.karyl.anifetch.types.LanguageType;

import java.util.Map;

public class Subtitle {
	private LanguageType language;
	private String ass;
	private String srt;

	public Subtitle(LanguageType language, String ass, String srt) {
		this.language = language;
		this.ass = ass;
		this.srt = srt;
	}

	public LanguageType getLanguage() {
		return language;
	}

	public void setLanguage(LanguageType language) {
		this.language = language;
	}
}
