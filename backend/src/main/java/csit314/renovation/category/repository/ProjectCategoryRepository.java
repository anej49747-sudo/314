package csit314.renovation.category.repository;

import csit314.renovation.category.entity.ProjectCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectCategoryRepository extends JpaRepository<ProjectCategory, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<ProjectCategory> findAllByOrderByNameAsc();
}
