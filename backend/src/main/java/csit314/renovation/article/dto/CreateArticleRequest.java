package csit314.renovation.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateArticleRequest(
        @NotBlank @Size(min = 5, max = 120) String title,
        @NotBlank @Size(min = 20, max = 240) String summary,
        @NotBlank @Size(min = 100, max = 10000) String content) { }
