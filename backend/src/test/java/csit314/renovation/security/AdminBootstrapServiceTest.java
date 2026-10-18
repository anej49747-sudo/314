package csit314.renovation.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {
    @Mock UserAccountRepository repository;
    @Mock PasswordEncoder encoder;
    @InjectMocks AdminBootstrapService bootstrap;

    @Test
    void createsOneAdminWithHashAndNoHardcodedCredential() {
        when(encoder.encode("ConfigPassword1!")).thenReturn("$argon2id$hashed");
        bootstrap.ensureCreated("  ADMIN@EXAMPLE.COM ", "ConfigPassword1!");
        ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).saveAndFlush(saved.capture());
        assertEquals("admin@example.com", saved.getValue().getEmail());
        assertEquals(UserRole.USER_ADMIN, saved.getValue().getRole());
        assertEquals("$argon2id$hashed", saved.getValue().getPasswordHash());
    }

    @Test
    void existingAdminIsNotModified() {
        when(repository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(
                UserAccount.newUserAdmin("Admin", "admin@example.com", "existing-hash")));
        bootstrap.ensureCreated("admin@example.com", "ConfigPassword1!");
        verify(repository, never()).saveAndFlush(any());
        verify(encoder, never()).encode(any());
    }

    @Test
    void existingCustomerIsNeverPromoted() {
        when(repository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(
                UserAccount.newCustomer(null, "Customer", "admin@example.com", "existing-hash")));
        assertThrows(IllegalStateException.class, () -> bootstrap.ensureCreated("admin@example.com", "ConfigPassword1!"));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void invalidCredentialsFailWithoutPersisting() {
        assertThrows(IllegalStateException.class, () -> bootstrap.ensureCreated("bad-address", "weak"));
        verify(repository, never()).saveAndFlush(any());
    }
}
