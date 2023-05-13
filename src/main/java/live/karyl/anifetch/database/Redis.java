package live.karyl.anifetch.database;

import live.karyl.anifetch.AniFetchApplication;
import live.karyl.anifetch.config.ConfigManager;
import org.tinylog.Logger;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Redis {

	private JedisPool jedisPool;

	private final ConfigManager config = AniFetchApplication.getConfig();

	protected final String REDIS_SEARCH = "search";
	protected final String REDIS_SOURCE = "source";
	protected final String REDIS_NON_EXIST = "non_exist";

	public void init() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		var host = config.getRedisHost();
		var port = config.getRedisPort();
		var pass = config.getRedisPassword();
		var poolMax = config.getRedisPoolSizeMax();
		var poolMin = config.getRedisPoolSizeMin();
		var poolIdle = config.getRedisPoolSizeIdle();
		poolConfig.setMaxTotal(poolMax);
		poolConfig.setMaxIdle(poolIdle);
		poolConfig.setMinIdle(poolMin);

		jedisPool = new JedisPool(poolConfig, host, port, 5000, pass);

		if (!jedisPool.getResource().ping().equals("PONG")) {
			Logger.error("Redis is not connected");
		} else {
			Logger.info("Redis is connected");
		}
	}

	public void set(String key, String value, String type) {
		Logger.info("REDIS SET | {} - {}", key, type);
		try (var jedis = jedisPool.getResource()) {
			switch (type) {
				case REDIS_SEARCH -> {
					jedis.setex(REDIS_SEARCH + ":" + key, 43200, value); // 12 hours
				}
				case REDIS_SOURCE -> {
					jedis.setex(REDIS_SOURCE + ":" + key, 3600, value); // 1 hour
				}
				case REDIS_NON_EXIST -> {
					jedis.setex(REDIS_NON_EXIST + ":" + key, 604800, value); // 1 week
				}
			}
		}
	}

	public String get(String key, String type) {
		Logger.info("REDIS - GET | {} - {}", key, type);
		try (var jedis = jedisPool.getResource()) {
			switch (type) {
				case REDIS_SEARCH -> {
					return jedis.get(REDIS_SEARCH + ":" + key);
				}
				case REDIS_SOURCE -> {
					return jedis.get(REDIS_SOURCE + ":" + key);
				}
			}
		}
		return null;
	}

	public boolean exists(String key, String type) {
		Logger.info("REDIS - CHECK EXISTS | {} - {}", key, type);
		try (var jedis = jedisPool.getResource()) {
			switch (type) {
				case REDIS_SEARCH -> {
					return jedis.exists(REDIS_SEARCH + ":" + key);
				}
				case REDIS_SOURCE -> {
					return jedis.exists(REDIS_SOURCE + ":" + key);
				}
				case REDIS_NON_EXIST -> {
					return jedis.exists(REDIS_NON_EXIST + ":" + key);
				}
			}
		}
		return false;
	}

	public void delete(String key, String type) {
		try (var jedis = jedisPool.getResource()) {
			switch (type) {
				case REDIS_SEARCH -> {
					jedis.del(REDIS_SEARCH + ":" + key);
				}
				case REDIS_SOURCE -> {
					jedis.del(REDIS_SOURCE + ":" + key);
				}
				case REDIS_NON_EXIST -> {
					jedis.del(REDIS_NON_EXIST + ":" + key);
				}
			}
		}
	}

	public void deleteAll() {
		try (var jedis = jedisPool.getResource()) {
			jedis.keys(REDIS_SOURCE + ":*").forEach(jedis::del);
			jedis.keys(REDIS_SEARCH + ":*").forEach(jedis::del);
			jedis.keys(REDIS_NON_EXIST + ":*").forEach(jedis::del);
		}
	}
}
