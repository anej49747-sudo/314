package csit314.renovation.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AccountUserDetailsServiceTest {
    @Mock UserAccountRepository accounts;
    @InjectMocks AccountUserDetailsService service;

    @Test
    void loadsActiveCustomerWithCorrectAuthorityAndHash() {
        UserAccount account = UserAccount.newCustomer(null, "Jane", "jane@example.com", "$argon2id$hash");
        when(accounts.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(account));
        UserDetails details = service.loadUserByUsername("jane@example.com");
        assertEquals("jane@example.com", details.getUsername());
        assertEquals("$argon2id$hash", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream().anyMatch(auth -> "ROLE_CUSTOMER".equals(auth.getAuthority())));
    }

    @Test
    void neverCreatesCredentialsForUnknownAccount() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("unknown@example.com"));
    }
}
