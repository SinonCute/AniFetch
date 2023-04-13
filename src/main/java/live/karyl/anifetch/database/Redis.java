package live.karyl.anifetch.database;

import live.karyl.anifetch.AniFetchApplication;
import live.karyl.anifetch.config.ConfigManager;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Redis {

	private JedisPool jedisPool;

	private final ConfigManager config = AniFetchApplication.getConfig();

	public void init() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		var host = config.getRedisHost();
		var port = config.getRedisPort();
		var poolMax = config.getRedisPoolSizeMax();
		var poolMin = config.getRedisPoolSizeMin();
		var poolIdle = config.getRedisPoolSizeIdle();
		poolConfig.setMaxTotal(poolMax);
		poolConfig.setMaxIdle(poolIdle);
		poolConfig.setMinIdle(poolMin);

		jedisPool = new JedisPool(poolConfig, host, port);

		if (!jedisPool.getResource().ping().equals("PONG")) {
			System.out.println("Redis is not connected");
		} else {
			System.out.println("Redis is connected");
		}
	}

	public void set(String key, String value, String type) {
		System.out.println("SET | " + key + " - " + type + "");
		try (var jedis = jedisPool.getResource()) {
			switch (type) {
				case "search" -> {
					var prefix = "search:";
					jedis.setex(prefix + key, 43200, value); // 12 hours
				}
				case "source" -> {
					var prefix = "source:";
					jedis.setex(prefix + key, 3600, value); // 1 hour
				}
			}
		}
	}

	public String get(String key, String type) {
		System.out.println("GET | " + key + " - " + type + "");
		try (var jedis = jedisPool.getResource()) {
			switch (type) {
				case "search" -> {
					var prefix = "search:";
					return jedis.get(prefix + key);
				}
				case "source" -> {
					var prefix = "source:";
					return jedis.get(prefix + key);
				}
			}
		}
		return null;
	}

	public boolean exists(String key, String type) {
		System.out.println("EXISTS | " + key + " - " + type + "");
		try (var jedis = jedisPool.getResource()) {
			switch (type) {
				case "search" -> {
					var prefix = "search:";
					return jedis.exists(prefix + key);
				}
				case "source" -> {
					var prefix = "source:";
					return jedis.exists(prefix + key);
				}
			}
		}
		return false;
	}

	public void delete(String key, String type) {
		try (var jedis = jedisPool.getResource()) {
			switch (type) {
				case "search" -> {
					var prefix = "search:";
					jedis.del(prefix + key);
				}
				case "source" -> {
					var prefix = "source:";
					jedis.del(prefix + key);
				}
			}
		}
	}

	public void deleteAll() {
		try (var jedis = jedisPool.getResource()) {
			jedis.keys("search:*").forEach(jedis::del);
			jedis.keys("source:*").forEach(jedis::del);
		}
	}
}
