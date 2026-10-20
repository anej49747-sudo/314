package csit314.renovation.portfolio.repository;

import csit314.renovation.account.entity.UserRole;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteriorDesignProjectRepository extends JpaRepository<InteriorDesignProject, Long> {
    // Public discovery: only projects of active, admin-provisioned designers.
    @EntityGraph(attributePaths = {"designer", "category"})
    List<InteriorDesignProject> findByDesigner_RoleAndDesigner_ActiveTrueOrderByCreatedAtDescIdDesc(
            UserRole role);

    // Derived 'Containing' escapes LIKE wildcards in a visitor's literal search text.
    @EntityGraph(attributePaths = {"designer", "category"})
    List<InteriorDesignProject> findByDesigner_RoleAndDesigner_ActiveTrueAndTitleContainingIgnoreCaseOrderByCreatedAtDescIdDesc(
            UserRole role, String title);
    @EntityGraph(attributePaths = {"designer", "category"})
    Optional<InteriorDesignProject> findById(Long id);

    @EntityGraph(attributePaths = "category")
    Optional<InteriorDesignProject> findByIdAndDesigner_Id(Long id, Long designerId);

    @EntityGraph(attributePaths = "category")
    List<InteriorDesignProject> findByDesigner_IdOrderByCreatedAtDescIdDesc(Long designerId);
}
