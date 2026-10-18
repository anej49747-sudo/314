package csit314.renovation.designer.control;

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
import csit314.renovation.designer.dto.CreateDesignerRequest;
import csit314.renovation.designer.dto.CreatedDesignerResponse;
import csit314.renovation.registration.exception.DuplicateEmailException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DesignerAccountServiceTest {
    @Mock UserAccountRepository repository;
    @Mock PasswordEncoder encoder;
    @Mock InitialPasswordGenerator passwords;
    @Mock Authentication authentication;
    @InjectMocks DesignerAccountService service;

    private void activeAdmin() {
        when(authentication.getName()).thenReturn("admin@example.com");
        when(repository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(UserAccount.newUserAdmin("Admin", "admin@example.com", "stored")));
    }

    @Test
    void createsDesignerWithServerAssignedRoleAndOneTimePassword() {
        activeAdmin();
        when(passwords.generate()).thenReturn("GeneratedSecurePass123!");
        when(encoder.encode("GeneratedSecurePass123!")).thenReturn("$argon2id$hashed");
        when(repository.saveAndFlush(any(UserAccount.class))).thenAnswer(call -> call.getArgument(0));
        CreatedDesignerResponse response = service.create(
                new CreateDesignerRequest(Salutation.DR, "  Designer Co  ", "  DESIGN@EXAMPLE.COM  "), authentication);
        ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).existsByEmailIgnoreCase("design@example.com");
        verify(repository).saveAndFlush(saved.capture());
        assertEquals(UserRole.INTERIOR_DESIGNER, saved.getValue().getRole());
        assertEquals("Designer Co", saved.getValue().getName());
        assertEquals("$argon2id$hashed", saved.getValue().getPasswordHash());
        assertTrue(saved.getValue().isActive());
        assertEquals("Dr Designer Co", response.displayName());
        assertEquals("GeneratedSecurePass123!", response.initialPassword());
    }

    @Test
    void duplicateIsRejectedBeforeGeneratingPassword() {
        activeAdmin();
        when(repository.existsByEmailIgnoreCase("design@example.com")).thenReturn(true);
        assertThrows(DuplicateEmailException.class, () -> service.create(
                new CreateDesignerRequest(null, "Designer", "DESIGN@example.com"), authentication));
        verify(passwords, never()).generate();
    }

    @Test
    void databaseUniquenessRaceIsMappedToDuplicateError() {
        activeAdmin();
        when(passwords.generate()).thenReturn("GeneratedSecurePass123!");
        when(encoder.encode(any())).thenReturn("hash");
        when(repository.saveAndFlush(any(UserAccount.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThrows(DuplicateEmailException.class, () -> service.create(
                new CreateDesignerRequest(null, "Designer", "design@example.com"), authentication));
    }

    @Test
    void revokedAdminAccessCannotCreateAccounts() {
        when(authentication.getName()).thenReturn("admin@example.com");
        when(repository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(UserAccount.newCustomer(null, "Customer", "admin@example.com", "stored")));
        assertThrows(AccessDeniedException.class, () -> service.create(
                new CreateDesignerRequest(null, "Designer", "design@example.com"), authentication));
        verify(repository, never()).saveAndFlush(any());
    }
}
