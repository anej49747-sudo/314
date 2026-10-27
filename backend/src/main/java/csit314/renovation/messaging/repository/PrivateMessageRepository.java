package csit314.renovation.messaging.repository;

import csit314.renovation.messaging.entity.PrivateMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {
    List<PrivateMessage> findByConversation_IdOrderByIdDesc(Long conversationId, Pageable pageable);
    List<PrivateMessage> findByConversation_IdAndIdLessThanOrderByIdDesc(
            Long conversationId, Long beforeId, Pageable pageable);
}
