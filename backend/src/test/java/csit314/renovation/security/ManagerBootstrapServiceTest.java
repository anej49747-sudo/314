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
class ManagerBootstrapServiceTest {
    @Mock UserAccountRepository accounts;
    @Mock PasswordEncoder encoder;
    @InjectMocks ManagerBootstrapService bootstrap;

    @Test
    void createsManagerWithEncodedPassword() {
        when(encoder.encode("ConfigPassword1!")).thenReturn("$argon2id$hashed");
        bootstrap.ensureCreated(" MANAGER@EXAMPLE.COM ", "ConfigPassword1!");
        ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(accounts).saveAndFlush(saved.capture());
        assertEquals("manager@example.com", saved.getValue().getEmail());
        assertEquals(UserRole.PLATFORM_MANAGER, saved.getValue().getRole());
        assertEquals("$argon2id$hashed", saved.getValue().getPasswordHash());
    }

    @Test
    void doesNotPromoteAnExistingCustomer() {
        when(accounts.findByEmailIgnoreCase("manager@example.com")).thenReturn(Optional.of(
                UserAccount.newCustomer(null, "Customer", "manager@example.com", "existing-hash")));
        assertThrows(IllegalStateException.class,
                () -> bootstrap.ensureCreated("manager@example.com", "ConfigPassword1!"));
        verify(accounts, never()).saveAndFlush(any());
    }

    @Test
    void refusesInvalidPassword() {
        assertThrows(IllegalStateException.class,
                () -> bootstrap.ensureCreated("manager@example.com", "weak"));
        verify(accounts, never()).saveAndFlush(any());
    }
}
