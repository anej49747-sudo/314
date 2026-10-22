package csit314.renovation.account.dto;

import csit314.renovation.account.entity.UserAccount;

/** Only the fields needed to identify an account in the User Admin search results. */
public record AdminAccountSummaryResponse(Long id, String displayName, String email,
        String role, boolean active) {
    public static AdminAccountSummaryResponse fromAccount(UserAccount account) {
        return new AdminAccountSummaryResponse(account.getId(), account.getDisplayName(),
                account.getEmail(), account.getRole().name(), account.isActive());
    }
}
