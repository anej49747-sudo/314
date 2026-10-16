package csit314.renovation.authentication.dto;

import csit314.renovation.account.entity.UserAccount;

public record AccountProfileResponse(Long id, String displayName, String email, String role) {
    public static AccountProfileResponse fromAccount(UserAccount account) {
        return new AccountProfileResponse(account.getId(), account.getDisplayName(),
                account.getEmail(), account.getRole().name());
    }
}
