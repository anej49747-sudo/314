package csit314.renovation.recognition.repository;

import csit314.renovation.account.entity.UserRole;
import csit314.renovation.recognition.entity.DesignerAward;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignerAwardRepository extends JpaRepository<DesignerAward, Long> {
    boolean existsByDesigner_IdAndTitleKey(Long designerId, String titleKey);

    @EntityGraph(attributePaths = "designer")
    List<DesignerAward> findByDesigner_RoleAndDesigner_ActiveTrueOrderByAwardedAtDescIdDesc(UserRole role);
}
