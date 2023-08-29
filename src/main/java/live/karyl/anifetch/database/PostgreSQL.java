package live.karyl.anifetch.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import live.karyl.anifetch.AniFetchApplication;
import live.karyl.anifetch.config.ConfigManager;
import live.karyl.anifetch.models.AnimeParser;
import org.tinylog.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Properties;

public class PostgreSQL {

    private DataSource dataSource;

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
            hikariConfig.setMaximumPoolSize(12);
            hikariConfig.setMinimumIdle(12);
            hikariConfig.setMaxLifetime(1800000);
            hikariConfig.setKeepaliveTime(30000);
            hikariConfig.setDataSourceProperties(new Properties() {{
                put("cachePrepStmts", "true");
                put("prepStmtCacheSize", "250");
                put("prepStmtCacheSqlLimit", "2048");
                put("useServerPrepStmts", "true");
                put("useLocalSessionState", "true");
                put("useLocalTransactionState", "true");
                put("rewriteBatchedStatements", "true");
                put("cacheResultSetMetadata", "true");
                put("cacheServerConfiguration", "true");
                put("elideSetAutoCommits", "true");
                put("maintainTimeStats", "false");
            }});
            dataSource = new HikariDataSource(hikariConfig);
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
        try (Connection sql = dataSource.getConnection()) {
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
        try (Connection sql = dataSource.getConnection()) {
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
