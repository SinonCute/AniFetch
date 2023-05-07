package live.karyl.anifetch.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import live.karyl.anifetch.AniFetchApplication;
import live.karyl.anifetch.config.ConfigManager;
import live.karyl.anifetch.models.AnimeParser;
import org.tinylog.Logger;

import java.sql.*;

public class PostgreSQL {

    private Connection sql;

    private static final String INSERT_ANIME = "INSERT INTO anime (anime_id, provider_id, provider_name) VALUES (?, ?, ?)";

    private static final String SELECT_ANIME = "SELECT * FROM anime WHERE anime_id = ? AND provider_name = ?";

    private final ConfigManager config = AniFetchApplication.getConfig();

    public void init() {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl("jdbc:postgresql://%s:%s/%s"
                    .formatted(config.getDatabaseHost(), config.getDatabasePort(), config.getDatabaseName()));
            hikariConfig.setUsername(config.getDatabaseUser());
            hikariConfig.setPassword(config.getDatabasePassword());
            hikariConfig.setConnectionTimeout(config.getDatabaseTimeout());
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            HikariDataSource ds = new HikariDataSource(hikariConfig);
            sql = ds.getConnection();
            Logger.info("PostgreSQL connection established");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void cleanup(ResultSet result, Statement statement) {
        if (result != null) {
            try {
                result.close();
            } catch (SQLException e) {
                Logger.error("SQLException on cleanup", e);
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                Logger.error("SQLException on cleanup", e);
            }
        }
    }

    public void addAnimeFetch(AnimeParser animeParser) {
        String animeId = animeParser.getAnimeId();
        String providerId = animeParser.getMediaId();
        String provider = animeParser.getProviderName();
        PreparedStatement statement = null;
        try {
            statement = sql.prepareStatement(INSERT_ANIME);
            statement.setString(1, animeId);
            statement.setString(2, providerId);
            statement.setString(3, provider);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.error("SQLException on addAnime", e);
        }
    }

    public String getAnimeFetch(String animeId, String provider) {
        PreparedStatement statement;
        ResultSet result;
        try {
            statement = sql.prepareStatement(SELECT_ANIME);
            statement.setString(1, animeId);
            statement.setString(2, provider);
            result = statement.executeQuery();
            if (result.next()) {
                return result.getString("provider_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.error("SQLException on providerId", e);
        }
        return null;
    }

    public boolean checkAnimeFetchExists(String animeId, String provider) {
        return getAnimeFetch(animeId, provider) != null;
    }
}
