package csit314.renovation.design.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_design_elements")
public class DesignElement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false)
    private CustomerDesignDraft draft;

    @Column(nullable = false, length = 12)
    private String kind;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "x_cm", nullable = false)
    private int xCm;
    @Column(name = "y_cm", nullable = false)
    private int yCm;
    @Column(name = "width_cm", nullable = false)
    private int widthCm;
    @Column(name = "height_cm", nullable = false)
    private int heightCm;
    @Column(name = "rotation_deg", nullable = false)
    private int rotation;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected DesignElement() {}

    public DesignElement(String kind, String name, int xCm, int yCm, int widthCm,
            int heightCm, int rotation, int displayOrder) {
        this.kind = kind;
        this.name = name;
        this.xCm = xCm;
        this.yCm = yCm;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.rotation = rotation;
        this.displayOrder = displayOrder;
    }
    public void attach(CustomerDesignDraft owner) { this.draft = owner; }
    public String getKind() { return kind; }
    public String getName() { return name; }
    public int getXCm() { return xCm; }
    public int getYCm() { return yCm; }
    public int getWidthCm() { return widthCm; }
    public int getHeightCm() { return heightCm; }
    public int getRotation() { return rotation; }
}
