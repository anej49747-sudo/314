package csit314.renovation.registration.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import csit314.renovation.account.entity.Salutation;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.registration.dto.RegisterRequest;
import csit314.renovation.registration.dto.RegisterResponse;
import csit314.renovation.registration.exception.DuplicateEmailException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    @Mock UserAccountRepository repository;
    @Mock PasswordEncoder encoder;
    @Mock JdbcTemplate jdbc;
    @InjectMocks RegistrationService registrationService;

    @Test
    void normalizesEmailAndAssignsCustomerRoleAndHashedPassword() {
        RegisterRequest request = new RegisterRequest(Salutation.DR, "  Jane Tan  ", "  JANE@EXAMPLE.COM  ", "SecurePass1!");
        when(encoder.encode("SecurePass1!")).thenReturn("$argon2id$encoded");
        when(repository.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount created = invocation.getArgument(0);
            // Simulate the database-generated identity returned by saveAndFlush.
            ReflectionTestUtils.setField(created, "id", 101L);
            return created;
        });

        RegisterResponse response = registrationService.register(request);

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).existsByEmailIgnoreCase("jane@example.com");
        verify(repository).saveAndFlush(accountCaptor.capture());
        verify(jdbc).update(
                "INSERT INTO customer_recommendation_preferences(customer_id,onboarding_state) VALUES(?, 'PENDING')",
                101L);
        UserAccount account = accountCaptor.getValue();
        assertEquals(UserRole.CUSTOMER, account.getRole());
        assertEquals("Jane Tan", account.getName());
        assertEquals("jane@example.com", account.getEmail());
        assertEquals("$argon2id$encoded", account.getPasswordHash());
        assertTrue(account.isActive());
        assertEquals("Dr Jane Tan", response.displayName());
    }

    @Test
    void rejectsPreexistingEmailBeforeHashing() {
        when(repository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);
        assertThrows(DuplicateEmailException.class, () -> registrationService.register(
                new RegisterRequest(null, "Customer", "USER@EXAMPLE.COM", "SecurePass1!")));
        verify(encoder, never()).encode(any());
    }

    @Test
    void mapsDatabaseUniqueIndexRaceToConflict() {
        when(encoder.encode(any())).thenReturn("$argon2id$encoded");
        when(repository.saveAndFlush(any(UserAccount.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));
        assertThrows(DuplicateEmailException.class, () -> registrationService.register(
                new RegisterRequest(null, "Customer", "user@example.com", "SecurePass1!")));
    }
}
