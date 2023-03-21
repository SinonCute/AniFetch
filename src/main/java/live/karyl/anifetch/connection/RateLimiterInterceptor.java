package live.karyl.anifetch.connection;

import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class RateLimiterInterceptor implements Interceptor {
	private final int limit;
	private final int interval;
	private final Queue<Long> timestamps = new LinkedList<>();

	public RateLimiterInterceptor(int limit, int interval) {
		this.limit = limit;
		this.interval = interval;
	}

	@NotNull
	@Override
	public Response intercept(@NotNull Chain chain) throws IOException {
		while (timestamps.size() >= limit) {
			Logger.info("Rate limit reached, waiting for next interval");
			long oldestTimestamp = timestamps.peek();
			long elapsed  = System.currentTimeMillis() - oldestTimestamp;
			if (elapsed < interval) {
				try {
					Thread.sleep(interval - elapsed);
				} catch (InterruptedException e) {
					Logger.error(e, "Error when sleeping");
					e.printStackTrace();
				}
			}
			timestamps.remove();
		}
		timestamps.add(System.currentTimeMillis());
		return chain.proceed(chain.request());
	}
}