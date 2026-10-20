package csit314.renovation.designer.boundary;

import csit314.renovation.designer.control.DesignerProfileService;
import csit314.renovation.designer.dto.DesignerProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DesignerProfileController {
    private final DesignerProfileService service;

    public DesignerProfileController(DesignerProfileService service) {
        this.service = service;
    }

    @GetMapping("/api/designers/{designerId}")
    public DesignerProfileResponse getProfile(@PathVariable("designerId") Long designerId) {
        return service.findPublicProfile(designerId);
    }
}
