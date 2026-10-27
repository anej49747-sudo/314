package csit314.renovation.qa.repository;

import csit314.renovation.qa.entity.ProjectQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ProjectQuestionRepository extends JpaRepository<ProjectQuestion, Long> {
    @Query("""
        SELECT q FROM ProjectQuestion q
        WHERE q.project.id = :projectId AND q.answeredAt IS NOT NULL
          AND (:term = '' OR LOCATE(:term, LOWER(q.question)) > 0
                 OR LOCATE(:term, LOWER(q.answer)) > 0)
        """)
    Page<ProjectQuestion> publicAnswers(@Param("projectId") Long projectId,
            @Param("term") String term, Pageable page);

    Page<ProjectQuestion> findByCustomer_IdAndProject_IdAndAnsweredAtIsNull(
            Long customerId, Long projectId, Pageable page);

    @Query("""
        SELECT q FROM ProjectQuestion q
        WHERE q.project.designer.id = :designerId
          AND (:projectId IS NULL OR q.project.id = :projectId)
          AND (:status = 'all' OR (:status = 'pending' AND q.answeredAt IS NULL)
                    OR (:status = 'answered' AND q.answeredAt IS NOT NULL))
          AND (:term = '' OR LOCATE(:term, LOWER(q.question)) > 0
                    OR LOCATE(:term, LOWER(q.project.title)) > 0)
        """)
    Page<ProjectQuestion> designerQueue(@Param("designerId") Long designerId,
            @Param("projectId") Long projectId, @Param("status") String status,
            @Param("term") String term, Pageable page);

    long countByProject_Designer_Id(Long designerId);
    long countByProject_Designer_IdAndAnsweredAtIsNull(Long designerId);
    long countByProject_Designer_IdAndAnsweredAtIsNotNull(Long designerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM ProjectQuestion q WHERE q.id = :id")
    java.util.Optional<ProjectQuestion> lockForAnswer(@Param("id") Long id);
}
