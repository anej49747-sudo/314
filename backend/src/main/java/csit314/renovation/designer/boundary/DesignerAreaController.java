package csit314.renovation.designer.boundary;

import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.authentication.dto.AccountProfileResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal partner API; future designer features use the same server-side password gate. */
@RestController
@RequestMapping("/api/designer")
public class DesignerAreaController {
    private final AuthenticationService authenticationService;

    public DesignerAreaController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/area")
    public AccountProfileResponse area(Authentication authentication) {
        return authenticationService.currentDesigner(authentication);
    }
}
