package csit314.renovation.review.repository;

import csit314.renovation.review.entity.DesignerReview;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DesignerReviewRepository extends JpaRepository<DesignerReview, Long> {
    Slice<DesignerReview> findByDesigner_Id(Long designerId, Pageable pageable);

    @Query("select r.rating, count(r.id) from DesignerReview r "
            + "where r.designer.id = :designerId group by r.rating")
    List<Object[]> countRatingsByDesignerId(@Param("designerId") Long designerId);
}
