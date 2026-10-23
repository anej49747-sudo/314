package csit314.renovation.security;

import csit314.renovation.account.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revoke access on the next API request after suspension, including existing sessions. */
public class ActiveAccountSessionFilter extends OncePerRequestFilter {
    private final UserAccountRepository accounts;

    public ActiveAccountSessionFilter(UserAccountRepository accounts) { this.accounts = accounts; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (path.startsWith("/api/")
                && !path.equals("/api/login") && !path.equals("/api/logout")
                && !path.equals("/api/auth/csrf")
                && authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            boolean valid = accounts.findByEmailIgnoreCase(authentication.getName())
                    .filter(account -> account.isActive()
                            && authentication.getAuthorities().stream().anyMatch(authority ->
                                    ("ROLE_" + account.getRole().name()).equals(authority.getAuthority())))
                    .isPresent();
            if (!valid) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
