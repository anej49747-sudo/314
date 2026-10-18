package csit314.renovation.authentication.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.authentication.dto.ChangePasswordRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DesignerPasswordService {
    private final UserAccountRepository accounts;
    private final PasswordEncoder encoder;

    public DesignerPasswordService(UserAccountRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Designer account required");
        }
        UserAccount account = accounts.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Designer account required"));
        if (!account.isActive() || account.getRole() != UserRole.INTERIOR_DESIGNER) {
            throw new AccessDeniedException("Designer account required");
        }
        // This endpoint is only for first-login setup after a temporary credential was verified at login.
        // Do not allow an established account to change its password without reauthentication.
        if (!account.isPasswordChangeRequired()) {
            throw new AccessDeniedException("Initial password setup is not required");
        }
        if (encoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new PasswordChangeException("New password must be different from your initial password.");
        }
        account.replacePasswordHash(encoder.encode(request.newPassword()));
        // JPA dirty checking persists both the new hash and cleared requirement atomically.
    }
}
