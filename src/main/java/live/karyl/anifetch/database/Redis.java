package live.karyl.anifetch.database;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Redis {

	private JedisPool jedisPool;

	public void init() {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(128);
		config.setMaxIdle(128);
		config.setMinIdle(16);
		jedisPool = new JedisPool(config, "100.100.187.19", 6379, 10000);
		if (!isConnected()) {
			System.out.println("Redis is not connected");
		} else {
			System.out.println("Redis is connected");
		}
	}

	public void set(String key, String value, String type) {
		if (!isConnected()) return;
		System.out.println("SET | " + key + " - " + type + "");
		var jedis = jedisPool.getResource();
		switch (type) {
			case "search" -> {
				var prefix = "search:";
				jedis.set(prefix + key, value);
				jedis.expire(key, 43200); // 12 hours
			}
			case "source" -> {
				var prefix = "source:";
				jedis.set(prefix + key, value);
				jedis.expire(key, 3600); // 1 hour
			}
		}
	}

	public String get(String key, String type) {
		if (!isConnected()) return null;
		System.out.println("GET | " + key + " - " + type + "");
		var jedis = jedisPool.getResource();
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
		return null;
	}

	public boolean exists(String key, String type) {
		if (!isConnected()) return false;
		System.out.println("EXISTS | " + key + " - " + type + "");
		var jedis = jedisPool.getResource();
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
		return false;
	}

	public void delete(String key, String type) {
		var jedis = jedisPool.getResource();
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

	public void deleteAll() {
		var jedis = jedisPool.getResource();
		jedis.del("search:*");
		jedis.del("source:*");
	}

	public boolean isConnected() {
		return jedisPool.getResource().isConnected();
	}
}
