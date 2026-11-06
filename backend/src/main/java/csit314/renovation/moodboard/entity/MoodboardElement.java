package csit314.renovation.moodboard.entity;

import csit314.renovation.portfolio.entity.ProjectImage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "customer_moodboard_elements")
public class MoodboardElement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private CustomerMoodboard board;

    @Column(nullable = false, length = 12)
    private String kind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id")
    private ProjectImage photo;

    @Column(name = "source_label", length = 180)
    private String sourceLabel;

    @Column(name = "text_content", length = 180)
    private String textContent;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 7)
    private String color;

    @Column(name = "font_size")
    private Integer fontSize;

    private int x;
    private int y;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "layer_order", nullable = false)
    private int layerOrder;

    protected MoodboardElement() {}

    public MoodboardElement(String kind, ProjectImage photo, String sourceLabel, String textContent,
            String color, Integer fontSize, int x, int y, int width, int height, int layerOrder) {
        this.kind = kind;
        this.photo = photo;
        this.sourceLabel = sourceLabel;
        this.textContent = textContent;
        this.color = color;
        this.fontSize = fontSize;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.layerOrder = layerOrder;
    }

    void attachTo(CustomerMoodboard owner) { this.board = owner; }
    public String getKind() { return kind; }
    public ProjectImage getPhoto() { return photo; }
    public String getSourceLabel() { return sourceLabel; }
    public String getTextContent() { return textContent; }
    public String getColor() { return color; }
    public Integer getFontSize() { return fontSize; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getLayerOrder() { return layerOrder; }
}
