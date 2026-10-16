package csit314.renovation.registration.dto;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import java.time.Instant;

public record RegisterResponse(Long id, String displayName, String email, UserRole role, Instant createdAt) {
    public static RegisterResponse fromAccount(UserAccount account) {
        return new RegisterResponse(account.getId(), account.getDisplayName(), account.getEmail(),
                account.getRole(), account.getCreatedAt());
    }
}
