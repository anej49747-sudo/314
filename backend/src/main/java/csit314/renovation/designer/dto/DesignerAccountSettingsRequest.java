package csit314.renovation.designer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DesignerAccountSettingsRequest(
        @NotBlank(message = "Display name is required")
        @Size(max = 160, message = "Display name must be at most 160 characters")
        String displayName,
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email) {}
