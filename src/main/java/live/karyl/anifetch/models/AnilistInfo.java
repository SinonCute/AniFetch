package live.karyl.anifetch.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class AnilistInfo {

	private String id;
	private Title title;
	private int malId;
	private String[] synonyms;
	private boolean isLicensed;
	private boolean isAdult;
	private String countryOfOrigin;
	private Trailer trailer;
	private String image;
	private int popularity;
	private String color;
	private String cover;
	private String description;
	private String status;
	private int releaseDate;
	private StartDate startDate;
	private EndDate endDate;
	private NextAiringEpisode nextAiringEpisode;
	private int totalEpisodes;
	private int currentEpisode;
	private int rating;
	private int duration;
	private String[] genres;
	private String season;
	private String[] studios;
	private ArrayList<Character> characters;
	private ArrayList<Relation> relations;
	private List<Episode> episodes;
	private String subOrDub;
	private String type;
	private Mappings mappings;

	public static AnilistInfo fromJson(String json) {
		Gson gson = new Gson();
		return gson.fromJson(json, AnilistInfo.class);
	}

	public String getId() {
		return id;
	}

	public Title getTitle() {
		return title;
	}

	public int getMalId() {
		return malId;
	}

	public String[] getSynonyms() {
		return synonyms;
	}

	public boolean isLicensed() {
		return isLicensed;
	}

	public boolean isAdult() {
		return isAdult;
	}

	public String getCountryOfOrigin() {
		return countryOfOrigin;
	}

	public Trailer getTrailer() {
		return trailer;
	}

	public String getImage() {
		return image;
	}

	public int getPopularity() {
		return popularity;
	}

	public String getColor() {
		return color;
	}

	public String getCover() {
		return cover;
	}

	public String getDescription() {
		return description;
	}

	public String getStatus() {
		return status;
	}

	public int getReleaseDate() {
		return releaseDate;
	}

	public StartDate getStartDate() {
		return startDate;
	}

	public EndDate getEndDate() {
		return endDate;
	}

	public int getTotalEpisodes() {
		return totalEpisodes;
	}

	public int getCurrentEpisode() {
		return currentEpisode;
	}

	public int getRating() {
		return rating;
	}

	public int getDuration() {
		return duration;
	}

	public String[] getGenres() {
		return genres;
	}

	public String getSeason() {
		return season;
	}

	public String[] getStudios() {
		return studios;
	}

	public String getSubOrDub() {
		return subOrDub;
	}

	public String getType() {
		return type;
	}

	public ArrayList<Character> getCharacters() {
		return characters;
	}

	public Mappings getMappings() {
		return mappings;
	}

	public List<Episode> getEpisodes() {
		return episodes;
	}

	public ArrayList<Relation> getRelation() { return relations; }

	public static class Title {
		public String romaji;
		public String english;
		@SerializedName("native")
		public String nativeTitle;
	}

	public class Name{
		public String first;
		public String last;
		public String full;
		@JsonProperty("native")
		public String mynative;
		public String userPreferred;
	}

	public class Trailer{
		public String id;
		public String site;
		public String thumbnail;
	}


	public class StartDate{
		public int year;
		public int month;
		public int day;
	}

	public class EndDate{
		public int year;
		public int month;
		public int day;
	}

	public static class NextAiringEpisode {
		public int airingTime;
		public int timeUntilAiring;
		public int episode;
	}

	public class Mappings {
		@SerializedName("mal")
		public int mal;

		@SerializedName("anidb")
		public int anidb;

		@SerializedName("kitsu")
		public int kitsu;

		@SerializedName("anilist")
		public int anilist;

		@SerializedName("thetvdb")
		public int thetvdb;

		@SerializedName("anisearch")
		public int anisearch;

		@SerializedName("livechart")
		public int livechart;

		@SerializedName("notify.moe")
		public String notifyMoe;

		@SerializedName("anime-planet")
		public String animePlanet;
	}

	public class Character {
		public int id;
		public String role;
		public Name name;
		public String image;
		public ArrayList<VoiceActor> voiceActors;
	}

	public class VoiceActor {
		public int id;
		public String language;
		public Name name;
		public String image;
	}

	public class Relation {
		public int id;
		public String relationType;
		public int malId;
		public Title title;
		public String status;
		public int episodes;
		public String image;
		public String color;
		public String type;
		public String cover;
		public int rating;
	}

	public class Episode {
		public String id;
		public String title;
		public String description;
		public double number;
		public String image;
		public String airDate;
	}
}


