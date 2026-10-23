package csit314.renovation.account.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record AdminAccountSearchResponse(List<AdminAccountSummaryResponse> accounts,
        int page, int size, long totalElements, int totalPages) {
    public static AdminAccountSearchResponse fromPage(Page<AdminAccountSummaryResponse> matches) {
        return new AdminAccountSearchResponse(matches.getContent(), matches.getNumber(),
                matches.getSize(), matches.getTotalElements(), matches.getTotalPages());
    }
}
