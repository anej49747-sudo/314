package csit314.renovation.authentication.boundary;

import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.authentication.control.DesignerPasswordService;
import csit314.renovation.authentication.dto.AccountProfileResponse;
import csit314.renovation.authentication.dto.ChangePasswordRequest;
import csit314.renovation.authentication.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final DesignerPasswordService designerPasswordService;

    public AuthenticationController(AuthenticationService authenticationService,
            DesignerPasswordService designerPasswordService) {
        this.authenticationService = authenticationService;
        this.designerPasswordService = designerPasswordService;
    }

    public record CsrfResponse(String headerName, String token) {}

    @GetMapping("/auth/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    @PostMapping("/login")
    public AccountProfileResponse login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        return authenticationService.login(request, servletRequest, servletResponse);
    }

    @GetMapping("/auth/me")
    public AccountProfileResponse me(Authentication authentication) {
        return authenticationService.currentAccount(authentication);
    }

    @GetMapping("/auth/designer/me")
    public AccountProfileResponse designerMe(Authentication authentication) {
        return authenticationService.currentDesigner(authentication);
    }

    @PostMapping("/auth/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeDesignerPassword(@Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        designerPasswordService.changePassword(request, authentication);
    }

    @GetMapping("/auth/manager/me")
    public AccountProfileResponse managerMe(Authentication authentication) {
        return authenticationService.currentManager(authentication);
    }

    @GetMapping("/auth/admin/me")
    public AccountProfileResponse adminMe(Authentication authentication) {
        return authenticationService.currentAdmin(authentication);
    }
}
