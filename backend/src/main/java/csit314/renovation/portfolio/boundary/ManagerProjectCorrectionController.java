package csit314.renovation.portfolio.boundary;

import csit314.renovation.portfolio.control.ManagerProjectCorrectionService;
import csit314.renovation.portfolio.dto.ManagerProjectResponse;
import csit314.renovation.portfolio.dto.ManagerUpdateProjectRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/projects")
public class ManagerProjectCorrectionController {
    private final ManagerProjectCorrectionService service;

    public ManagerProjectCorrectionController(ManagerProjectCorrectionService service) {
        this.service = service;
    }

    @GetMapping
    public List<ManagerProjectResponse> list(Authentication authentication) {
        return service.list(authentication);
    }

    @PutMapping("/{projectId}")
    public ManagerProjectResponse correct(@PathVariable Long projectId,
            @Valid @RequestBody ManagerUpdateProjectRequest request, Authentication authentication) {
        return service.correct(projectId, request, authentication);
    }
}
