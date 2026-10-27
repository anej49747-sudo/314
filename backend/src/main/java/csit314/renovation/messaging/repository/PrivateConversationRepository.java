package csit314.renovation.messaging.repository;

import csit314.renovation.messaging.entity.PrivateConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivateConversationRepository extends JpaRepository<PrivateConversation, Long> {
    Optional<PrivateConversation> findByCustomer_IdAndDesigner_Id(Long customerId, Long designerId);
    List<PrivateConversation> findByCustomer_IdOrderByLastMessageAtDescIdDesc(Long customerId);
    List<PrivateConversation> findByDesigner_IdOrderByLastMessageAtDescIdDesc(Long designerId);
}
