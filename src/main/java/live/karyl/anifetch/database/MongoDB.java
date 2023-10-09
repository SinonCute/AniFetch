package live.karyl.anifetch.database;

import com.google.gson.Gson;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import live.karyl.anifetch.config.ConfigManager;
import live.karyl.anifetch.models.AnimeMapping;
import org.apache.commons.lang3.time.StopWatch;
import org.bson.Document;
import org.tinylog.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class MongoDB {
    private final ConfigManager config = ConfigManager.getInstance();

    MongoDatabase database;

    public void init() {
        MongoClient mongoClient = MongoClients.create(
                MongoClientSettings.builder()
                        .applyConnectionString(
                                new ConnectionString("mongodb://{user}:{pass}@{host}:{port}/?directConnection=true&serverSelectionTimeoutMS=2000&appName=LicenseServer"
                                        .replace("{user}", config.getDatabaseUser())
                                        .replace("{pass}", URLEncoder.encode(config.getDatabasePassword(), StandardCharsets.UTF_8))
                                        .replace("{host}", config.getDatabaseHost())
                                        .replace("{port}", config.getDatabasePort()))
                        ).build()
        );
        database = mongoClient.getDatabase(config.getDatabaseName());
        Logger.info("MongoDB connection established");
    }

    public void addAnimeMapping(AnimeMapping animeMapping) {
        StopWatch stopWatch = StopWatch.createStarted();
        var collection = database.getCollection("anime");
        var animeDocument = Document.parse(new Gson().toJson(animeMapping));
        collection.insertOne(animeDocument);
        stopWatch.stop();
        Logger.info("Added anime " + animeMapping.animeId() + " to database ({}ms)",stopWatch.getTime());
    }

    public AnimeMapping getAnimeMapping(String animeId) {
        StopWatch stopWatch = StopWatch.createStarted();
        var collection = database.getCollection("anime");
        var document = collection.find(new Document("animeId", animeId)).first();
        if (document == null) return new AnimeMapping(animeId, new HashMap<>());
        stopWatch.stop();
        Logger.info("Got anime " + animeId + " from database ({}ms)",stopWatch.getTime());
        return new Gson().fromJson(document.toJson(), AnimeMapping.class);
    }

    public void updateAnimeMapping(AnimeMapping animeMapping) {
        StopWatch stopWatch = StopWatch.createStarted();
        var collection = database.getCollection("anime");
        var document = collection.find(new Document("animeId", animeMapping.animeId())).first();
        if (document == null) return;
        var animeDocument = Document.parse(new Gson().toJson(animeMapping));
        collection.replaceOne(document, animeDocument);
        stopWatch.stop();
        Logger.info("Updated anime " + animeMapping.animeId() + " in database ({}ms)",stopWatch.getTime());
    }
}
