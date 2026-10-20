package csit314.renovation.security;

import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Server-side gate for all designer API features while the initial password remains temporary. */
public class DesignerPasswordGateFilter extends OncePerRequestFilter {
    private static final Set<String> ALLOWED_WHILE_PENDING = Set.of(
            "/api/login", "/api/logout", "/api/auth/csrf", "/api/auth/designer/me",
            "/api/auth/change-password");
    private final UserAccountRepository accounts;

    public DesignerPasswordGateFilter(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        String path = request.getRequestURI()
            .substring(request.getContextPath().length());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (path.startsWith("/api/") && !("GET".equals(request.getMethod())
                && ("/api/designers".equals(path) || "/api/projects".equals(path)
                        || path.matches("/api/designers/[0-9]+")
                        || path.matches("/api/projects/[0-9]+")))
                && !ALLOWED_WHILE_PENDING.contains(path)
                && authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_INTERIOR_DESIGNER".equals(authority.getAuthority()))) {
            boolean allowed = accounts.findByEmailIgnoreCase(authentication.getName())
                    .filter(account -> account.isActive() && account.getRole() == UserRole.INTERIOR_DESIGNER
                            && !account.isPasswordChangeRequired())
                    .isPresent();
            if (!allowed) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
