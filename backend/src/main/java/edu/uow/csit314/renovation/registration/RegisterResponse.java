package edu.uow.csit314.renovation.registration;

import edu.uow.csit314.renovation.account.UserAccount;
import edu.uow.csit314.renovation.account.UserRole;
import java.time.Instant;

public record RegisterResponse(Long id, String displayName, String email, UserRole role, Instant createdAt) {
    public static RegisterResponse fromAccount(UserAccount account) {
        return new RegisterResponse(account.getId(), account.getDisplayName(), account.getEmail(),
                account.getRole(), account.getCreatedAt());
    }
}
