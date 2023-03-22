package live.karyl.anifetch.database;

import redis.clients.jedis.Jedis;

public class Redis {

	private Jedis jedis;

	public void init() {
		jedis = new Jedis("localhost", 6379);
		System.out.println("Redis connection established");
	}

	public void set(String key, String value, String type) {
		System.out.println("Setting " + key + " to " + value);
		switch (type) {
			case "search" -> {
				var prefix = "search:";
				jedis.set(prefix + key, value);
				jedis.expire(key, 43200); // 12 hours
			}
			case "provider" -> {
				var prefix = "provider:";
				jedis.set(prefix + key, value);
			}
			case "source" -> {
				var prefix = "source:";
				jedis.set(prefix + key, value);
				jedis.expire(key, 3600); // 1 hour
			}
		}
	}

	public String get(String key, String type) {
		System.out.println("Getting " + key + " from redis");
		switch (type) {
			case "search" -> {
				var prefix = "search:";
				return jedis.get(prefix + key);
			}
			case "provider" -> {
				var prefix = "provider:";
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
		System.out.println("Checking if " + key + " exists");
		switch (type) {
			case "search" -> {
				var prefix = "search:";
				return jedis.exists(prefix + key);
			}
			case "provider" -> {
				var prefix = "provider:";
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
		switch (type) {
			case "search" -> {
				var prefix = "search:";
				jedis.del(prefix + key);
			}
			case "provider" -> {
				var prefix = "provider:";
				jedis.del(prefix + key);
			}
			case "source" -> {
				var prefix = "source:";
				jedis.del(prefix + key);
			}
		}
	}

	public void deleteAll() {
		jedis.del("search:*");
		jedis.del("provider:*");
		jedis.del("source:*");
	}

}
