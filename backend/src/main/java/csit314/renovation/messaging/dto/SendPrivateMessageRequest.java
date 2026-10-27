package csit314.renovation.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendPrivateMessageRequest(@NotBlank @Size(max = 2000) String body) { }
