package csit314.renovation.security;

import csit314.renovation.account.repository.UserAccountRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        // OWASP baseline: 19 MiB, 2 iterations, parallelism 1; tune for deployment hardware.
        return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfRepository) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfRepository)));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            SecurityContextRepository contextRepository, CsrfTokenRepository csrfRepository,
            UserAccountRepository accounts) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(csrfRepository)
                        // Public signup does not authenticate a session; keep its original API contract.
                        .ignoringRequestMatchers("/api/register"))
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(cache -> cache.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/register", "/api/login", "/api/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/auth/session").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/designers",
                                "/api/designers/{designerId}", "/api/projects", "/api/projects/{projectId}",
                                "/api/projects/*/images/*")
                        .permitAll()
                        // #34/#35 public recognitions and manager-only selection/grant operations.
                        .requestMatchers(HttpMethod.GET, "/api/recognitions").permitAll()
                        .requestMatchers("/api/manager/recognitions/**").hasRole("PLATFORM_MANAGER")
                        // #8 public article reading; cover bytes use the same opaque image store.
                        .requestMatchers(HttpMethod.GET, "/api/articles", "/api/articles/*",
                                "/api/articles/*/cover").permitAll()
                        // #36 only Platform Managers can publish articles (session + CSRF).
                        .requestMatchers(HttpMethod.POST, "/api/manager/articles").hasRole("PLATFORM_MANAGER")
                        // Story #52: the saved-project list is private to the signed-in customer.
                        .requestMatchers(HttpMethod.GET, "/api/customer/account")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/api/customer/account")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/customer/account")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/customer/favourites")
                        .hasRole("CUSTOMER")
                        // Story #22: only a signed-in customer can inspect or change their saves.
                        .requestMatchers(HttpMethod.GET, "/api/customer/favourites/*")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/api/customer/favourites/*")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/customer/favourites/*")
                        .hasRole("CUSTOMER")
                        // Story #10: customer reviews are public, but never reveal customer identities.
                        .requestMatchers(HttpMethod.GET, "/api/designers/*/reviews").permitAll()
                        // Story #46: submitting reviews requires an authenticated customer + CSRF.
                        .requestMatchers(HttpMethod.POST, "/api/designers/*/reviews")
                        .hasRole("CUSTOMER")
                        // #43 Customer reporting; #42 User Admin-only report decisions and queue.
                        .requestMatchers(HttpMethod.POST, "/api/designers/*/reports")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/admin/designer-reports",
                                "/api/admin/designer-reports/*")
                        .hasRole("USER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/designer-reports/*/decision")
                        .hasRole("USER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/auth/manager/me").hasRole("PLATFORM_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/categories").hasRole("PLATFORM_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/manager/projects")
                        .hasRole("PLATFORM_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/*")
                        .hasRole("PLATFORM_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/manager/projects/*")
                        .hasRole("PLATFORM_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/manager/reports/activity")
                        .hasRole("PLATFORM_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/auth/admin/me", "/api/admin/accounts",
                                "/api/admin/accounts/*/login-history")
                        .hasRole("USER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/accounts/*")
                        .hasRole("USER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/accounts/*/suspend",
                                "/api/admin/accounts/*/reactivate")
                        .hasRole("USER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/auth/designer/me", "/api/designer/area")
                        .hasRole("INTERIOR_DESIGNER")
                        .requestMatchers(HttpMethod.GET, "/api/designer/account")
                        .hasRole("INTERIOR_DESIGNER")
                        .requestMatchers(HttpMethod.PUT, "/api/designer/account")
                        .hasRole("INTERIOR_DESIGNER")
                        .requestMatchers(HttpMethod.GET, "/api/designer/company-information", "/api/designer/projects")
                        .hasRole("INTERIOR_DESIGNER")
                        .requestMatchers(HttpMethod.GET, "/api/designer/projects/*/images/*")
                        .hasRole("INTERIOR_DESIGNER")
                        .requestMatchers(HttpMethod.POST, "/api/designer/projects")
                        .hasRole("INTERIOR_DESIGNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/designer/projects/*")
                        .hasRole("INTERIOR_DESIGNER")
                        .requestMatchers(HttpMethod.PUT, "/api/designer/projects/*")
                        .hasRole("INTERIOR_DESIGNER")
                        .requestMatchers(HttpMethod.PUT, "/api/designer/company-information")
                        .hasRole("INTERIOR_DESIGNER")
                        .requestMatchers(HttpMethod.POST, "/api/auth/change-password").hasRole("INTERIOR_DESIGNER")
                        .requestMatchers(HttpMethod.POST, "/api/admin/designers").hasRole("USER_ADMIN")
                        .anyRequest().denyAll());
        http.addFilterBefore(new DesignerPasswordGateFilter(accounts), AuthorizationFilter.class);
        http.addFilterBefore(new ActiveAccountSessionFilter(accounts), DesignerPasswordGateFilter.class);
        return http.build();
    }
}
