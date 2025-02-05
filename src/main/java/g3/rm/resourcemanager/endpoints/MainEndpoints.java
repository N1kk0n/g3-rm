package g3.rm.resourcemanager.endpoints;

import g3.rm.resourcemanager.routing.dtos.kafka.Content;
import g3.rm.resourcemanager.routing.services.RouterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainEndpoints {

    @Autowired
    private RouterService routerService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        routerService.createRoute("G3-TEST", new Content());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}