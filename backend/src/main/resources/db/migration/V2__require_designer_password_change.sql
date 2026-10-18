-- Track one-time designer credentials introduced in Story #37.
-- Existing users are unaffected; designers provisioned before this migration must change password.
ALTER TABLE user_accounts
    ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE user_accounts
SET password_change_required = TRUE
WHERE role = 'INTERIOR_DESIGNER';
