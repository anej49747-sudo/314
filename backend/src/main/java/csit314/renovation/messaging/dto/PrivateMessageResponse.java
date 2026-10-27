package csit314.renovation.messaging.dto;

import csit314.renovation.messaging.entity.PrivateMessage;
import java.time.Instant;

public record PrivateMessageResponse(Long id, Long conversationId, Long senderId,
        String body, Instant createdAt) {
    public static PrivateMessageResponse from(PrivateMessage message) {
        return new PrivateMessageResponse(message.getId(), message.getConversation().getId(),
                message.getSender().getId(), message.getBody(), message.getCreatedAt());
    }
}
