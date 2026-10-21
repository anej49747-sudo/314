package csit314.renovation.customer.boundary;

import csit314.renovation.customer.control.CustomerAccountSettingsService;
import csit314.renovation.customer.dto.CustomerAccountSettingsRequest;
import csit314.renovation.customer.dto.CustomerAccountSettingsResponse;
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
@RequestMapping("/api/customer/account")
public class CustomerAccountSettingsController {
    private final CustomerAccountSettingsService service;

    public CustomerAccountSettingsController(CustomerAccountSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public CustomerAccountSettingsResponse get(Authentication authentication) {
        return service.get(authentication);
    }

    @PutMapping
    public CustomerAccountSettingsResponse update(@Valid @RequestBody CustomerAccountSettingsRequest request,
            Authentication authentication, HttpServletRequest servletRequest) {
        CustomerAccountSettingsResponse saved = service.update(request, authentication);
        if (saved.signInRequired()) {
            // Our session principal uses the email address; the old one must not remain active.
            if (servletRequest.getSession(false) != null) {
                servletRequest.getSession(false).invalidate();
            }
            SecurityContextHolder.clearContext();
        }
        return saved;
    }
}
