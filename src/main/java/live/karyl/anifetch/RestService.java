package live.karyl.anifetch;

import jakarta.servlet.http.HttpServletRequest;
import live.karyl.anifetch.utils.DashPlayerCreator;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RestService {

    @GetMapping("/stream-bilibili")
    public ResponseEntity<String> streamBilibili(HttpServletRequest request, @RequestParam("id") String id) {
        if (id == null) {
            return ResponseEntity.badRequest().body("Missing id");
        }
//        var referer = request.getHeader("referer");
//        var origin = request.getHeader("origin");
//        if (referer == null || origin == null) {
//            return ResponseEntity.badRequest().body("Missing header");
//        }

        var videoSource = AniFetchApplication.getProviders().get("Bilibili").getLink(id);
        var xmlResponse = new DashPlayerCreator().generateDashXML(videoSource);
        return ResponseEntity.ok()
                //.header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Content-Type", "application/dash+xml")
                .body(xmlResponse);
    }
}
