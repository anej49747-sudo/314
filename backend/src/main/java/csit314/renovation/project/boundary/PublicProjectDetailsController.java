package csit314.renovation.project.boundary;

import csit314.renovation.project.control.PublicProjectDetailsService;
import csit314.renovation.project.dto.PublicProjectDetailsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicProjectDetailsController {
    private final PublicProjectDetailsService service;

    public PublicProjectDetailsController(PublicProjectDetailsService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{projectId}")
    public PublicProjectDetailsResponse getProject(@PathVariable("projectId") Long projectId) {
        return service.findPublicProject(projectId);
    }
}
