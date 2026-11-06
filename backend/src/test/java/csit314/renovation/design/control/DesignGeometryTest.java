package csit314.renovation.design.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import csit314.renovation.design.dto.DesignElementInput;
import csit314.renovation.design.entity.DesignElement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class DesignGeometryTest {
    @Test void measuredFloorBounds() {
        assertEquals(400, DesignGeometry.floor(400, "Width"));
        assertEquals(2500, DesignGeometry.floor(2500, "Height"));
        assertThrows(ResponseStatusException.class, () -> DesignGeometry.floor(399, "Width"));
        assertThrows(ResponseStatusException.class, () -> DesignGeometry.floor(2501, "Height"));
        assertThrows(ResponseStatusException.class, () -> DesignGeometry.floor(null, "Width"));
    }

    @Test void fitAndRotationAreValidated() {
        var valid = new DesignElementInput("FURNITURE", "Sofa", 100, 100, 210, 90, 90);
        assertEquals(90, DesignGeometry.checked(valid, 0, 1200, 800).getRotation());
        assertThrows(ResponseStatusException.class, () -> DesignGeometry.checked(
                new DesignElementInput("ROOM", "Bedroom", 0, 0, 300, 300, 90), 0, 1200, 800));
        assertThrows(ResponseStatusException.class, () -> DesignGeometry.checked(
                new DesignElementInput("ROOM", "Bedroom", 1100, 0, 300, 300, 0), 0, 1200, 800));
        assertThrows(ResponseStatusException.class, () -> DesignGeometry.checked(
                new DesignElementInput("FURNITURE", "Bed", -1, 0, 300, 300, 0), 0, 1200, 800));
        assertThrows(ResponseStatusException.class, () -> DesignGeometry.checked(
                new DesignElementInput("FURNITURE", "Bed", 0, 0, 300, 300, 360), 0, 1200, 800));
        assertThrows(ResponseStatusException.class, () -> DesignGeometry.checked(
                new DesignElementInput("ROOM", " ", 0, 0, 300, 300, 0), 0, 1200, 800));
        assertThrows(ResponseStatusException.class, () -> DesignGeometry.checked(
                new DesignElementInput("OPENING", "Window", 0, 0, 160, 24, 90), 0, 1200, 800));
    }

    @Test void touchingRoomsAreValidAndOverlapsAreWarnings() {
        DesignElement a = new DesignElement("ROOM", "Living", 0, 0, 500, 500, 0, 0);
        DesignElement b = new DesignElement("ROOM", "Kitchen", 500, 0, 200, 500, 0, 1);
        DesignElement furniture = new DesignElement("FURNITURE", "Sofa", 100, 100, 200, 90, 0, 2);
        assertEquals(0, DesignGeometry.roomOverlaps(List.of(a, b, furniture)));
        DesignElement c = new DesignElement("ROOM", "Bedroom", 400, 300, 200, 200, 0, 3);
        assertEquals(2, DesignGeometry.roomOverlaps(List.of(a, b, c, furniture)));
    }
}
