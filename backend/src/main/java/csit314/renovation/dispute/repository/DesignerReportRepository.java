package csit314.renovation.dispute.repository;

import csit314.renovation.dispute.entity.DesignerReport;
import csit314.renovation.dispute.entity.DesignerReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DesignerReportRepository extends JpaRepository<DesignerReport, Long> {
    boolean existsByCustomer_IdAndDesigner_IdAndStatus(Long customerId, Long designerId,
            DesignerReportStatus status);
    long countByStatus(DesignerReportStatus status);

    @Query("""
            SELECT report FROM DesignerReport report
            WHERE (:status IS NULL OR report.status = :status)
              AND (:query = '' OR LOCATE(:query, LOWER(report.designer.name)) > 0
                   OR LOCATE(:query, LOWER(report.customer.name)) > 0
                   OR EXISTS (SELECT company.designerId FROM DesignerCompany company
                       WHERE company.designerId = report.designer.id
                         AND LOCATE(:query, LOWER(company.companyName)) > 0)
                   OR (:refId IS NOT NULL AND report.id = :refId))
            """)
    Page<DesignerReport> search(@Param("status") DesignerReportStatus status,
            @Param("query") String query, @Param("refId") Long refId, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE designer_reports SET status = :status, decision_note = :note,
                decided_by = :adminId, decided_at = CURRENT_TIMESTAMP
            WHERE id = :reportId AND status = 'OPEN'
            """, nativeQuery = true)
    int closeOpenReport(@Param("reportId") Long reportId, @Param("status") String status,
            @Param("note") String note, @Param("adminId") Long adminId);
}
