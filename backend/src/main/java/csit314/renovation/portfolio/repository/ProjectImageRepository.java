package csit314.renovation.portfolio.repository;

import csit314.renovation.portfolio.entity.ProjectImage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {
    List<ProjectImage> findByProject_IdInOrderByDisplayOrderAscIdAsc(Collection<Long> projectIds);
    Optional<ProjectImage> findByIdAndProject_IdAndProject_Designer_Id(
            Long imageId, Long projectId, Long designerId);
}
