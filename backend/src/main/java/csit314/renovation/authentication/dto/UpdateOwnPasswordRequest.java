package csit314.renovation.authentication.dto;

import csit314.renovation.registration.validation.ValidRegistrationPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOwnPasswordRequest(
        @NotBlank @Size(max = 128) String currentPassword,
        @ValidRegistrationPassword String newPassword) {}
