package csit314.renovation.portfolio.repository;

import csit314.renovation.portfolio.entity.InteriorDesignProject;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteriorDesignProjectRepository extends JpaRepository<InteriorDesignProject, Long> {
    @EntityGraph(attributePaths = "category")
    List<InteriorDesignProject> findByDesigner_IdOrderByCreatedAtDescIdDesc(Long designerId);
}
