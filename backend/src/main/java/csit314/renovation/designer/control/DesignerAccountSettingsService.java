package csit314.renovation.designer.control;

import csit314.renovation.account.control.AccountIdentitySettingsService;
import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.designer.dto.DesignerAccountSettingsRequest;
import csit314.renovation.designer.dto.DesignerAccountSettingsResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DesignerAccountSettingsService {
    private final AccountIdentitySettingsService identities;

    public DesignerAccountSettingsService(AccountIdentitySettingsService identities) {
        this.identities = identities;
    }

    @Transactional(readOnly = true)
    public DesignerAccountSettingsResponse get(Authentication authentication) {
        return DesignerAccountSettingsResponse.from(
                identities.requireAccount(authentication, UserRole.INTERIOR_DESIGNER), false);
    }

    @Transactional
    public DesignerAccountSettingsResponse update(DesignerAccountSettingsRequest request,
            Authentication authentication) {
        UserAccount designer = identities.requireAccount(authentication, UserRole.INTERIOR_DESIGNER);
        boolean changedEmail = identities.update(designer, request.displayName(), request.email());
        return DesignerAccountSettingsResponse.from(designer, changedEmail);
    }
}
