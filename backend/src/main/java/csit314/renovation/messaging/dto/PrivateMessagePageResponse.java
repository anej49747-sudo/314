package csit314.renovation.messaging.dto;

import java.util.List;

public record PrivateMessagePageResponse(List<PrivateMessageResponse> messages, boolean hasMore) { }
