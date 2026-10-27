package csit314.renovation.qa.dto;

import java.util.List;

public record ProjectQuestionPageResponse(List<ProjectQuestionResponse> items,
        long total, int page, int totalPages) { }
