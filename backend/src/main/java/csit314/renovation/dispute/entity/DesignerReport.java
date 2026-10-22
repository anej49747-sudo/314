package csit314.renovation.dispute.entity;

import csit314.renovation.account.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "designer_reports")
public class DesignerReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserAccount customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "designer_id", nullable = false)
    private UserAccount designer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DesignerReportReason reason;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DesignerReportStatus status = DesignerReportStatus.OPEN;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private UserAccount decidedBy;

    @Column(name = "decision_note", length = 1000)
    private String decisionNote;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected DesignerReport() {}

    public static DesignerReport submit(UserAccount customer, UserAccount designer,
            DesignerReportReason reason, String description) {
        DesignerReport report = new DesignerReport();
        report.customer = customer;
        report.designer = designer;
        report.reason = reason;
        report.description = description;
        return report;
    }

    @PrePersist
    void setSubmittedAt() {
        if (submittedAt == null) submittedAt = Instant.now();
    }

    public void close(DesignerReportStatus outcome, String note, UserAccount admin) {
        if (status != DesignerReportStatus.OPEN) throw new IllegalStateException("Report is already closed");
        if (outcome == DesignerReportStatus.OPEN) throw new IllegalArgumentException("Invalid report outcome");
        this.status = outcome;
        this.decisionNote = note;
        this.decidedBy = admin;
        this.decidedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserAccount getCustomer() { return customer; }
    public UserAccount getDesigner() { return designer; }
    public DesignerReportReason getReason() { return reason; }
    public String getDescription() { return description; }
    public DesignerReportStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public UserAccount getDecidedBy() { return decidedBy; }
    public String getDecisionNote() { return decisionNote; }
    public Instant getDecidedAt() { return decidedAt; }
}
