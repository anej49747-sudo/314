package csit314.renovation.project.boundary;

import csit314.renovation.project.control.PublicProjectSearchService;
import csit314.renovation.project.dto.PublicProjectSearchResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicProjectSearchController {
    private final PublicProjectSearchService service;

    public PublicProjectSearchController(PublicProjectSearchService service) {
        this.service = service;
    }

    @GetMapping("/api/projects")
    public List<PublicProjectSearchResponse> searchProjects(
            @RequestParam(name = "q", required = false) String query) {
        return service.findProjects(query);
    }
}
