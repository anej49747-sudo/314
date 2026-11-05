package csit314.renovation.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerProjectQuestionRequest(@NotBlank @Size(max = 2000) String answer) { }
