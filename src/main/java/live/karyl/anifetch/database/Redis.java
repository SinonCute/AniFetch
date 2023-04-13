package live.karyl.anifetch.database;

import redis.clients.jedis.Jedis;


public class Redis {

	private Jedis jedis;

	public void init() {
		jedis = new Jedis("100.127.255.19", 6379);
		if (!jedis.ping().equals("PONG")) {
			System.out.println("Redis is not connected");
		} else {
			System.out.println("Redis is connected");
		}
	}

	public void set(String key, String value, String type) {
		System.out.println("SET | " + key + " - " + type + "");
		switch (type) {
			case "search" -> {
				var prefix = "search:";
				jedis.setex(prefix + key, 43200, value); // 12 hours
			}
			case "source" -> {
				var prefix = "source:";
				jedis.setex(prefix + key, 43200, value); // 12 hours
			}
		}
		jedis.close();
	}

	public String get(String key, String type) {
		System.out.println("GET | " + key + " - " + type + "");
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
		jedis.close();
		return null;
	}

	public boolean exists(String key, String type) {
		System.out.println("EXISTS | " + key + " - " + type + "");
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
		jedis.close();
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
		jedis.close();
	}

	public void deleteAll() {
		jedis.keys("search:*").forEach(jedis::del);
		jedis.keys("source:*").forEach(jedis::del);
		jedis.close();
	}

}
