package csit314.renovation.designer.dto;

import csit314.renovation.account.entity.UserAccount;
import java.time.Instant;

// The initial password is returned only in this creation response, never from a GET endpoint.
public record CreatedDesignerResponse(Long id, String displayName, String email, String role,
        boolean active, Instant createdAt, String initialPassword) {
    public static CreatedDesignerResponse fromAccount(UserAccount account, String initialPassword) {
        return new CreatedDesignerResponse(account.getId(), account.getDisplayName(), account.getEmail(),
                account.getRole().name(), account.isActive(), account.getCreatedAt(), initialPassword);
    }
}
