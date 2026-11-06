package csit314.renovation.moodboard.repository;

import csit314.renovation.moodboard.entity.CustomerMoodboard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerMoodboardRepository extends JpaRepository<CustomerMoodboard, Long> {
    List<CustomerMoodboard> findByCustomer_IdOrderByUpdatedAtDescIdDesc(Long customerId);
    Optional<CustomerMoodboard> findByIdAndCustomer_Id(Long boardId, Long customerId);
    long countByCustomer_Id(Long customerId);
}
