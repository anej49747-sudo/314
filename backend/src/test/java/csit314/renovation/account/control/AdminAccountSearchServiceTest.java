package csit314.renovation.account.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.authentication.control.AuthenticationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AdminAccountSearchServiceTest {
    @Mock UserAccountRepository accounts;
    @Mock AuthenticationService authenticationService;
    @Mock Authentication authentication;
    @InjectMocks AdminAccountSearchService search;

    @Test
    void appliesCanonicalSearchAndAlwaysRestrictsAccountRoles() {
        when(accounts.searchEligibleAccounts(any(), eq(UserRole.CUSTOMER), eq(true), eq("alex"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 20), 0));
        var result = search.search("  ALEX  ", "CUSTOMER", "active", 2, authentication);
        assertEquals(2, result.page());
        assertEquals(20, result.size());
        verify(authenticationService).currentAdmin(authentication);
        verify(accounts).searchEligibleAccounts(eq(List.of(UserRole.CUSTOMER, UserRole.INTERIOR_DESIGNER)),
                eq(UserRole.CUSTOMER), eq(true), eq("alex"), any(Pageable.class));
    }

    @Test
    void rejectsInvalidFiltersWithoutQueryingDatabase() {
        assertThrows(IllegalArgumentException.class,
                () -> search.search("", "USER_ADMIN", null, 0, authentication));
        assertThrows(IllegalArgumentException.class,
                () -> search.search("", null, "suspended", 0, authentication));
        assertThrows(IllegalArgumentException.class,
                () -> search.search("", null, null, -1, authentication));
        assertThrows(IllegalArgumentException.class,
                () -> search.search("x".repeat(101), null, null, 0, authentication));
        verify(accounts, never()).searchEligibleAccounts(any(), any(), any(), any(), any());
    }
}
