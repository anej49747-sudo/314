package csit314.renovation.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspendAccountRequest(@NotBlank @Size(max = 500) String reason) {}
