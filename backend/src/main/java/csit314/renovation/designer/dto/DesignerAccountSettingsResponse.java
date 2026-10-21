package csit314.renovation.designer.dto;

import csit314.renovation.account.entity.UserAccount;

/** Only editable account fields, never password hashes or role/approval data. */
public record DesignerAccountSettingsResponse(String displayName, String email, boolean signInRequired) {
    public static DesignerAccountSettingsResponse from(UserAccount account, boolean signInRequired) {
        // getName() avoids adding the salutation a second time when editing.
        return new DesignerAccountSettingsResponse(account.getName(), account.getEmail(), signInRequired);
    }
}
