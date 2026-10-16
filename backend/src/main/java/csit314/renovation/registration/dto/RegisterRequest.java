package csit314.renovation.registration.dto;

import csit314.renovation.account.entity.Salutation;
import csit314.renovation.registration.validation.ValidRegistrationPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        Salutation salutation,
        @NotBlank(message = "Name is required")
        @Size(max = 160, message = "Name must be at most 160 characters")
        String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,
        @ValidRegistrationPassword
        String password) {}
