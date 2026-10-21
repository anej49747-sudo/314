package csit314.renovation.customer.control;

import csit314.renovation.account.control.AccountIdentitySettingsService;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.customer.dto.CustomerAccountSettingsRequest;
import csit314.renovation.customer.dto.CustomerAccountSettingsResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerAccountSettingsService {
    private final AccountIdentitySettingsService identities;

    public CustomerAccountSettingsService(AccountIdentitySettingsService identities) {
        this.identities = identities;
    }

    @Transactional(readOnly = true)
    public CustomerAccountSettingsResponse get(Authentication authentication) {
        return CustomerAccountSettingsResponse.from(
                identities.requireAccount(authentication, UserRole.CUSTOMER), false);
    }

    @Transactional
    public CustomerAccountSettingsResponse update(CustomerAccountSettingsRequest request,
            Authentication authentication) {
        UserAccount customer = identities.requireAccount(authentication, UserRole.CUSTOMER);
        boolean changedEmail = identities.update(customer, request.displayName(), request.email());
        return CustomerAccountSettingsResponse.from(customer, changedEmail);
    }
}
