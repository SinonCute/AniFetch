package live.karyl.anifetch.connection;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.tinylog.Logger;

import java.util.concurrent.TimeUnit;

public class OkHttp {

    private OkHttpClient client;

    public void init() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.readTimeout(15, TimeUnit.SECONDS);
        builder.connectTimeout(15, TimeUnit.SECONDS);
        //builder.addInterceptor(new RateLimiterInterceptor(60, 60000));
        client = builder.build();
    }

    public Response call(Request request) {
        var retry = 5;
        while (retry > 0) {
            try {
                return client.newCall(request).execute();
            } catch (Exception e) {
                Logger.debug("Retry: " + retry + " " + request.url());
                retry--;
            }
        }
        return null;
    }

    public Response callWithoutRateLimit(Request request) {
        var retry = 5;
        while (retry > 0) {
            try {
                return client.newCall(request).execute();
            } catch (Exception e) {
                Logger.debug("Retry: " + retry + " " + request.url());
                retry--;
            }
        }
        return null;
    }
}
