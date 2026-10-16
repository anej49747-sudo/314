package edu.uow.csit314.renovation.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.uow.csit314.renovation.account.Salutation;
import edu.uow.csit314.renovation.account.UserAccount;
import edu.uow.csit314.renovation.account.UserAccountRepository;
import edu.uow.csit314.renovation.account.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    @Mock UserAccountRepository repository;
    @Mock PasswordEncoder encoder;
    @InjectMocks RegistrationService registrationService;

    @Test
    void normalizesEmailAndAssignsCustomerRoleAndHashedPassword() {
        RegisterRequest request = new RegisterRequest(Salutation.DR, "  Jane Tan  ", "  JANE@EXAMPLE.COM  ", "SecurePass1!");
        when(encoder.encode("SecurePass1!")).thenReturn("$argon2id$encoded");
        when(repository.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = registrationService.register(request);

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).existsByEmailIgnoreCase("jane@example.com");
        verify(repository).saveAndFlush(accountCaptor.capture());
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
