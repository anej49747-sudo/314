package csit314.renovation.authentication.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.authentication.dto.AccountProfileResponse;
import csit314.renovation.authentication.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository accounts;
    private final SecurityContextRepository contextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;

    public AuthenticationService(AuthenticationManager authenticationManager,
            UserAccountRepository accounts, SecurityContextRepository contextRepository,
            SessionAuthenticationStrategy sessionStrategy) {
        this.authenticationManager = authenticationManager;
        this.accounts = accounts;
        this.contextRepository = contextRepository;
        this.sessionStrategy = sessionStrategy;
    }

    public AccountProfileResponse login(LoginRequest request, HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));

        UserAccount account = accounts.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        // The account is checked again because only current, active accounts may start sessions.
        if (!account.isActive()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        // Custom JSON authentication does not automatically run Spring Security's session strategies.
        // Rotate session ID (fixation defense) and invalidate the pre-login CSRF token.
        sessionStrategy.onAuthentication(authentication, servletRequest, servletResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // Spring Security 6 requires explicit persistence for programmatic authentication.
        contextRepository.saveContext(context, servletRequest, servletResponse);
        return AccountProfileResponse.fromAccount(account);
    }

    public AccountProfileResponse currentAdmin(Authentication authentication) {
        UserAccount account = authenticatedAccount(authentication);
        if (account.getRole() != UserRole.USER_ADMIN) {
            throw new AccessDeniedException("Administrator access required");
        }
        return AccountProfileResponse.fromAccount(account);
    }

    public AccountProfileResponse currentDesigner(Authentication authentication) {
        UserAccount account = authenticatedAccount(authentication);
        if (account.getRole() != UserRole.INTERIOR_DESIGNER) {
            throw new AccessDeniedException("Designer access required");
        }
        return AccountProfileResponse.fromAccount(account);
    }

    public AccountProfileResponse currentAccount(Authentication authentication) {
        return AccountProfileResponse.fromAccount(authenticatedAccount(authentication));
    }

    private UserAccount authenticatedAccount(Authentication authentication) {
        UserAccount account = accounts.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!account.isActive()) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return account;
    }
}
