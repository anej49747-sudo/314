package csit314.renovation.authentication.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.authentication.dto.AccountProfileResponse;
import csit314.renovation.authentication.dto.LoginRequest;
import org.springframework.security.authentication.DisabledException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock AuthenticationManager manager;
    @Mock org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Mock UserAccountRepository accounts;
    @Mock SecurityContextRepository contexts;
    @Mock SessionAuthenticationStrategy sessions;
    @Mock LoginAttemptRecorder recorder;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @InjectMocks AuthenticationService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesAndExplicitlyPersistsSessionContext() {
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated("jane@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        when(manager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(accounts.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(
                UserAccount.newCustomer(null, "Jane", "jane@example.com", "$argon2id$hash")));

        AccountProfileResponse result = service.login(new LoginRequest(" JANE@EXAMPLE.COM ", "SecurePass1!"),
                request, response);

        assertEquals("jane@example.com", result.email());
        assertEquals("CUSTOMER", result.role());
        verify(recorder).record("jane@example.com", true);
        verify(sessions).onAuthentication(authenticated, request, response);
        verify(contexts).saveContext(any(SecurityContext.class), any(HttpServletRequest.class),
                any(HttpServletResponse.class));
        assertEquals(authenticated, SecurityContextHolder.getContext().getAuthentication());
    }


    @Test
    void matchingPasswordForSuspendedAccountShowsSuspensionWithoutStartingSession() {
        UserAccount suspended = UserAccount.newCustomer(null, "Jane", "jane@example.com", "encoded-password");
        suspended.setActiveForModeration(false);
        when(manager.authenticate(any(Authentication.class))).thenThrow(new DisabledException("Disabled"));
        when(accounts.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(suspended));
        when(encoder.matches("correct-password", "encoded-password")).thenReturn(true);

        assertThrows(SuspendedAccountException.class, () -> service.login(
                new LoginRequest("jane@example.com", "correct-password"), request, response));
        verify(recorder).record("jane@example.com", false);
        org.mockito.Mockito.verifyNoInteractions(contexts, sessions);
    }

    @Test
    void wrongPasswordForSuspendedAccountRemainsGeneric() {
        UserAccount suspended = UserAccount.newCustomer(null, "Jane", "jane@example.com", "encoded-password");
        suspended.setActiveForModeration(false);
        when(manager.authenticate(any(Authentication.class))).thenThrow(new DisabledException("Disabled"));
        when(accounts.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(suspended));
        when(encoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> service.login(
                new LoginRequest("jane@example.com", "wrong-password"), request, response));
        verify(recorder).record("jane@example.com", false);
        org.mockito.Mockito.verifyNoInteractions(contexts, sessions);
    }

    @Test
    void doesNotPersistSessionWhenAuthenticationFails() {
        when(manager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Invalid"));
        assertThrows(BadCredentialsException.class, () -> service.login(
                new LoginRequest("jane@example.com", "bad"), request, response));
        verify(recorder).record("jane@example.com", false);
        org.mockito.Mockito.verifyNoInteractions(contexts, sessions);
    }
}
