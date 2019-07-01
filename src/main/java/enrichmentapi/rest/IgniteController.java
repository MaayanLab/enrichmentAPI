package enrichmentapi.rest;

import org.apache.ignite.Ignite;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class IgniteController {


    private final Ignite ignite;

    public IgniteController(Ignite ignite) {
        this.ignite = ignite;
    }

    @GetMapping("/activate")
    public void activateCluster() {
        ignite.cluster().active(true);
    }

}
