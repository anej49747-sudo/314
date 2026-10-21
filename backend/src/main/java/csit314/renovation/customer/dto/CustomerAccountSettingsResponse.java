package csit314.renovation.customer.dto;

import csit314.renovation.account.entity.UserAccount;

/** Public account settings response; does not expose role, hash, or internal account ID. */
public record CustomerAccountSettingsResponse(String displayName, String email, boolean signInRequired) {
    public static CustomerAccountSettingsResponse from(UserAccount account, boolean signInRequired) {
        // The editable name excludes the optional salutation.
        return new CustomerAccountSettingsResponse(account.getName(), account.getEmail(), signInRequired);
    }
}
