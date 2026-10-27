package csit314.renovation.messaging.dto;

import java.time.Instant;

public record PrivateConversationResponse(Long id, Long counterpartId,
        String counterpartName, String lastMessagePreview, Instant lastMessageAt) { }
