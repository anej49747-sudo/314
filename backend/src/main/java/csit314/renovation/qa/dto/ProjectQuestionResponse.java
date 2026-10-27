package csit314.renovation.qa.dto;

import csit314.renovation.qa.entity.ProjectQuestion;
import java.time.Instant;

// Deliberately omits the customer's identifier and identity even from designer views.
public record ProjectQuestionResponse(Long id, Long projectId, String projectTitle,
        String question, String answer, Instant createdAt, Instant answeredAt) {
    public static ProjectQuestionResponse from(ProjectQuestion question) {
        return new ProjectQuestionResponse(question.getId(), question.getProject().getId(),
                question.getProject().getTitle(), question.getQuestion(), question.getAnswer(),
                question.getCreatedAt(), question.getAnsweredAt());
    }
}
