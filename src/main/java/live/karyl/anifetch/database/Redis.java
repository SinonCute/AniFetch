package live.karyl.anifetch.database;

import redis.clients.jedis.Jedis;

public class Redis {

	private Jedis jedis;

	public void init() {
		jedis = new Jedis("localhost", 6379);
		//jedis.connect();
		if (!jedis.isConnected()) {
			System.out.println("Redis is not connected");
		} else {
			System.out.println("Redis is connected");
		}
	}

	public void set(String key, String value, String type) {
		if (!jedis.isConnected()) return;
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
		if (!jedis.isConnected()) return null;
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
		if (!jedis.isConnected()) return false;
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
		jedis.flushAll();
	}

}
