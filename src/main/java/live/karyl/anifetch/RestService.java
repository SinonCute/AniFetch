package live.karyl.anifetch;

import jakarta.servlet.http.HttpServletRequest;
import live.karyl.anifetch.utils.DashPlayerCreator;
import live.karyl.anifetch.utils.JSONToVTTConverter;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Controller
public class RestService {

    @GetMapping("/stream-bilibili")
    public ResponseEntity<String> streamBilibili(@RequestParam("id") String id) {
        if (id == null) {
            return ResponseEntity.badRequest().body("Missing id");
        }

        var videoSource = AniFetchApplication.getProviders().get("Bilibili").getLink(id, false);
        var xmlResponse = new DashPlayerCreator().generateDashXML(videoSource);
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Content-Type", "application/dash+xml")
                .body(xmlResponse);
    }

    @GetMapping("/sub-bilibili")
    public ResponseEntity<String> subBilibili(@RequestParam("link") String link) throws IOException {
        if (link == null) {
            return ResponseEntity.badRequest().body("Missing id");
        }
        Request request = new Request.Builder()
                .url(URLDecoder.decode(link, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        Response response = AniFetchApplication.getConnection().callWithoutRateLimit(request);
        String result = new String(response.body().bytes(), StandardCharsets.UTF_8);
        if (response.code() != 200) {
            response.close();
            return ResponseEntity.badRequest().body("Failed to fetch subtitle");
        }
        String vtt = JSONToVTTConverter.convert(result);
        return ResponseEntity.ok()
                .header("Content-Type", "text/vtt; charset=utf-8")
                .body(vtt);
    }
}
