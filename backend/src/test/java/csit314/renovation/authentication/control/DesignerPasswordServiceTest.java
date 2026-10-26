package csit314.renovation.authentication.control;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.authentication.dto.ChangePasswordRequest;
import csit314.renovation.authentication.dto.UpdateOwnPasswordRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DesignerPasswordServiceTest {
    @Mock UserAccountRepository accounts;
    @Mock PasswordEncoder encoder;
    @Mock Authentication authentication;
    @InjectMocks DesignerPasswordService service;

    private UserAccount designer() {
        UserAccount account = UserAccount.newInteriorDesigner(null, "Designer", "designer@example.com", "storedHash");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("designer@example.com");
        when(accounts.findByEmailIgnoreCase("designer@example.com")).thenReturn(Optional.of(account));
        return account;
    }

    @Test
    void changesHashAndClearsFirstLoginRequirement() {
        UserAccount account = designer();
        when(encoder.encode("NewPassword1!")).thenReturn("newHash");
        service.changePassword(new ChangePasswordRequest("NewPassword1!"), authentication);
        assertFalse(account.isPasswordChangeRequired());
        assertTrue("newHash".equals(account.getPasswordHash()));
    }

    @Test
    void designerCannotRepeatInitialPasswordSetup() {
        UserAccount account = designer();
        account.replacePasswordHash("updatedHash");
        assertThrows(AccessDeniedException.class, () -> service.changePassword(
                new ChangePasswordRequest("AnotherPass1!"), authentication));
        verify(encoder, never()).encode(anyString());
    }

    @Test
    void existingPasswordCannotBeReused() {
        UserAccount account = designer();
        when(encoder.matches("Temporary1!", "storedHash")).thenReturn(true);
        assertThrows(PasswordChangeException.class, () -> service.changePassword(
                new ChangePasswordRequest("Temporary1!"), authentication));
        assertTrue(account.isPasswordChangeRequired());
    }

    @Test
    void customerCannotChangeDesignerPassword() {
        UserAccount account = UserAccount.newCustomer(null, "Customer", "customer@example.com", "storedHash");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("customer@example.com");
        when(accounts.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(account));
        assertThrows(AccessDeniedException.class, () -> service.changePassword(
                new ChangePasswordRequest("NewPassword1!"), authentication));
        verify(encoder, never()).encode(anyString());
    }
    @Test
    void establishedDesignerCanUpdatePasswordWithCurrentPassword() {
        UserAccount account = designer();
        account.replacePasswordHash("establishedHash");
        when(encoder.matches("OldPassword1!", "establishedHash")).thenReturn(true);
        when(encoder.encode("NewPassword2!")).thenReturn("newHash");
        service.updateOwnPassword(new UpdateOwnPasswordRequest("OldPassword1!", "NewPassword2!"), authentication);
        assertTrue("newHash".equals(account.getPasswordHash()));
    }

    @Test
    void rejectsWrongCurrentPasswordWithoutChangingHash() {
        UserAccount account = designer();
        account.replacePasswordHash("establishedHash");
        assertThrows(PasswordChangeException.class, () -> service.updateOwnPassword(
                new UpdateOwnPasswordRequest("WrongPassword1!", "NewPassword2!"), authentication));
        assertTrue("establishedHash".equals(account.getPasswordHash()));
        verify(encoder, never()).encode(anyString());
    }

    @Test
    void establishedCustomerCanChangePasswordWithCurrentPassword() {
        UserAccount customer = UserAccount.newCustomer(null, "Customer", "customer@example.com", "storedHash");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("customer@example.com");
        when(accounts.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));
        when(encoder.matches("OldPassword1!", "storedHash")).thenReturn(true);
        when(encoder.encode("NewPassword2!")).thenReturn("newCustomerHash");
        service.updateOwnPassword(new UpdateOwnPasswordRequest("OldPassword1!", "NewPassword2!"), authentication);
        assertTrue("newCustomerHash".equals(customer.getPasswordHash()));
    }

    @Test
    void customerCannotChangePasswordWithIncorrectCurrentPassword() {
        UserAccount customer = UserAccount.newCustomer(null, "Customer", "customer@example.com", "storedHash");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("customer@example.com");
        when(accounts.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));
        assertThrows(PasswordChangeException.class, () -> service.updateOwnPassword(
                new UpdateOwnPasswordRequest("BadPassword1!", "NewPassword2!"), authentication));
        assertTrue("storedHash".equals(customer.getPasswordHash()));
    }

}
