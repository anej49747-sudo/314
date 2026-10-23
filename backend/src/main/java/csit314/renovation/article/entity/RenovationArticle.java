package csit314.renovation.article.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "renovation_articles")
public class RenovationArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 240)
    private String summary;

    @Column(nullable = false, length = 10000)
    private String content;

    @Column(name = "cover_storage_key", length = 80, unique = true)
    private String coverStorageKey;

    @Column(name = "cover_content_type", length = 32)
    private String coverContentType;

    @Column(name = "published_at", nullable = false, updatable = false)
    private Instant publishedAt;

    protected RenovationArticle() { }

    public RenovationArticle(String title, String summary, String content, String coverStorageKey,
            String coverContentType) {
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.coverStorageKey = coverStorageKey;
        this.coverContentType = coverContentType;
    }

    @PrePersist
    void publishNow() {
        if (publishedAt == null) publishedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public String getCoverStorageKey() { return coverStorageKey; }
    public String getCoverContentType() { return coverContentType; }
    public Instant getPublishedAt() { return publishedAt; }
}
