package csit314.renovation.qa.dto;

import java.util.List;

public record DesignerQuestionQueueResponse(List<ProjectQuestionResponse> items, long total,
        int page, int totalPages, long pendingCount, long answeredCount, long allCount) { }
