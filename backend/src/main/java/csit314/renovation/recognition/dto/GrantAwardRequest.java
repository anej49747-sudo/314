package csit314.renovation.recognition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record GrantAwardRequest(@NotNull @Positive Long designerId,
        @NotBlank @Size(min = 3, max = 70) String title) { }
