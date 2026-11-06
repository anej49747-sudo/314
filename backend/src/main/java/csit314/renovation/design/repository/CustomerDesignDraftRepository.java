package csit314.renovation.design.repository;

import csit314.renovation.design.entity.CustomerDesignDraft;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerDesignDraftRepository extends JpaRepository<CustomerDesignDraft, Long> {
    List<CustomerDesignDraft> findByCustomer_IdOrderByUpdatedAtDescIdDesc(Long customerId);
    Optional<CustomerDesignDraft> findByIdAndCustomer_Id(Long draftId, Long customerId);
    long countByCustomer_Id(Long customerId);
}
