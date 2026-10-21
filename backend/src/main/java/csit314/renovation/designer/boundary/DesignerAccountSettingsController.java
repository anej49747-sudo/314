package csit314.renovation.designer.boundary;

import csit314.renovation.designer.control.DesignerAccountSettingsService;
import csit314.renovation.designer.dto.DesignerAccountSettingsRequest;
import csit314.renovation.designer.dto.DesignerAccountSettingsResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/designer/account")
public class DesignerAccountSettingsController {
    private final DesignerAccountSettingsService service;

    public DesignerAccountSettingsController(DesignerAccountSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public DesignerAccountSettingsResponse get(Authentication authentication) {
        return service.get(authentication);
    }

    @PutMapping
    public DesignerAccountSettingsResponse update(@Valid @RequestBody DesignerAccountSettingsRequest request,
            Authentication authentication, HttpServletRequest servletRequest) {
        DesignerAccountSettingsResponse saved = service.update(request, authentication);
        if (saved.signInRequired()) {
            // Existing sessions have an email-based principal; do not retain a stale identity.
            if (servletRequest.getSession(false) != null) {
                servletRequest.getSession(false).invalidate();
            }
            SecurityContextHolder.clearContext();
        }
        return saved;
    }
}
