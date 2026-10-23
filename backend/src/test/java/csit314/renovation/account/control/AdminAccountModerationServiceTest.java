package csit314.renovation.account.control;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import csit314.renovation.account.entity.AccountModerationEvent;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.repository.AccountModerationEventRepository;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.authentication.dto.AccountProfileResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AdminAccountModerationServiceTest {
    @Mock UserAccountRepository accounts;
    @Mock AccountModerationEventRepository events;
    @Mock AuthenticationService authenticationService;
    @Mock Authentication authentication;
    @InjectMocks AdminAccountModerationService service;

    @Test
    void rejectsBlankReasonBeforeMutation() {
        assertThrows(IllegalArgumentException.class, () -> service.suspend(22L, "   ", authentication));
        org.mockito.Mockito.verifyNoInteractions(accounts, events);
    }

    @Test
    void onlyCustomerOrDesignerMayBeSuspended() {
        UserAccount admin = UserAccount.newUserAdmin("Admin", "admin@example.com", "hash");
        when(authenticationService.currentAdmin(authentication))
                .thenReturn(new AccountProfileResponse(1L, "Admin", "admin@example.com", "USER_ADMIN", false));
        when(accounts.findById(1L)).thenReturn(Optional.of(admin));
        when(accounts.findById(2L)).thenReturn(Optional.of(admin));
        assertThrows(AccountNotFoundException.class, () -> service.suspend(2L, "Reason", authentication));
        org.mockito.Mockito.verifyNoInteractions(events);
    }
}
