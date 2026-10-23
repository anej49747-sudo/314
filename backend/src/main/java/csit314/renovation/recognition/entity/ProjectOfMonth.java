package csit314.renovation.recognition.entity;

import csit314.renovation.portfolio.entity.InteriorDesignProject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "project_of_month")
public class ProjectOfMonth {
    @Id
    @Column(name = "feature_month", length = 7)
    private String month;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private InteriorDesignProject project;

    @Column(name = "selected_at", nullable = false)
    private Instant selectedAt;

    protected ProjectOfMonth() { }

    public ProjectOfMonth(String month, InteriorDesignProject project) {
        this.month = month;
        this.project = project;
        this.selectedAt = Instant.now();
    }

    public void replace(InteriorDesignProject project) {
        this.project = project;
        this.selectedAt = Instant.now();
    }

    @PrePersist
    void beforePersist() {
        if (selectedAt == null) selectedAt = Instant.now();
    }

    public String getMonth() { return month; }
    public InteriorDesignProject getProject() { return project; }
    public Instant getSelectedAt() { return selectedAt; }
}
