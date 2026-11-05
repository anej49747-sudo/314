package csit314.renovation.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskProjectQuestionRequest(@NotBlank @Size(min = 10, max = 600) String question) { }
