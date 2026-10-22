package csit314.renovation.account.dto;

import csit314.renovation.account.entity.UserLoginEvent;
import java.time.Instant;

public record LoginHistoryItemResponse(Long id, Instant occurredAt, boolean successful) {
    public static LoginHistoryItemResponse fromEvent(UserLoginEvent event) {
        return new LoginHistoryItemResponse(event.getId(), event.getOccurredAt(), event.isSuccessful());
    }
}
