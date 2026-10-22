package csit314.renovation.account.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record LoginHistoryResponse(List<LoginHistoryItemResponse> events,
        int page, int size, long totalElements, int totalPages) {
    public static LoginHistoryResponse fromPage(Page<LoginHistoryItemResponse> matches) {
        return new LoginHistoryResponse(matches.getContent(), matches.getNumber(),
                matches.getSize(), matches.getTotalElements(), matches.getTotalPages());
    }
}
