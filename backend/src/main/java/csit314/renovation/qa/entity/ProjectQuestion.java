package csit314.renovation.qa.entity;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.portfolio.entity.InteriorDesignProject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "project_questions")
public class ProjectQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private InteriorDesignProject project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserAccount customer;

    @Column(nullable = false, length = 600)
    private String question;

    @Column(length = 2000)
    private String answer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "answered_at")
    private Instant answeredAt;

    protected ProjectQuestion() { }

    public ProjectQuestion(InteriorDesignProject project, UserAccount customer, String question) {
        this.project = project;
        this.customer = customer;
        this.question = question;
    }

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public void publishAnswer(String content) {
        if (answer != null) throw new IllegalStateException("Already answered");
        this.answer = content;
        this.answeredAt = Instant.now();
    }

    public Long getId() { return id; }
    public InteriorDesignProject getProject() { return project; }
    public UserAccount getCustomer() { return customer; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAnsweredAt() { return answeredAt; }
}
