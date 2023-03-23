package live.karyl.anifetch.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import live.karyl.anifetch.models.AnimeParser;
import org.tinylog.Logger;

import java.sql.*;

public class PostgreSQL {

    private Connection sql;

    private static final String INSERT_ANIME = "INSERT INTO anime (anime_id, provider_id, provider) VALUES (?, ?, ?) ON CONFLICT (anime_id) DO UPDATE SET provider_id = EXCLUDED.provider_id, provider = EXCLUDED.provider;";

    private static final String SELECT_ANIME = "SELECT * FROM anime WHERE anime_id = ? AND provider = ?";

    public void init() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:postgresql://db.myvhzegeowsapjobfptt.supabase.co:5432/postgres");
            config.setUsername("postgres");
            config.setPassword("Hiencaokgkg@@");
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(10000);
            HikariDataSource ds = new HikariDataSource(config);
            sql = ds.getConnection();
            System.out.println("PostgreSQL connection established");
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
        String providerId = animeParser.getProviderId();
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
        } finally {
            cleanup(null, statement);
        }
    }

    public String getAnimeFetch(String animeId, String provider) {
        PreparedStatement statement = null;
        ResultSet result = null;
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
        } finally {
            cleanup(result, statement);
        }
        return null;
    }

    public boolean checkAnimeFetchExists(String animeId, String provider) {
        return getAnimeFetch(animeId, provider) != null;
    }
}
