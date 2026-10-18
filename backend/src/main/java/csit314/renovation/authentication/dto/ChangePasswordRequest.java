package csit314.renovation.authentication.dto;

import csit314.renovation.registration.validation.ValidRegistrationPassword;

public record ChangePasswordRequest(
        @ValidRegistrationPassword String newPassword) {}
