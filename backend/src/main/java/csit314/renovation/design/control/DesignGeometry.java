package csit314.renovation.design.control;

import csit314.renovation.design.dto.DesignElementInput;
import csit314.renovation.design.entity.DesignElement;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Spatial constraints in integer centimetres; overlapping rooms are warnings, not destructive saves. */
public final class DesignGeometry {
    public static final int MIN_FLOOR_CM = 400;
    public static final int MAX_FLOOR_CM = 2500;
    public static final int MAX_ELEMENTS = 120;
    private DesignGeometry() {}

    public static int floor(Integer value, String name) {
        if (value == null || value < MIN_FLOOR_CM || value > MAX_FLOOR_CM) {
            throw bad(name + " must be between 4 and 25 m");
        }
        return value;
    }

    public static DesignElement checked(DesignElementInput input, int order, int floorW, int floorH) {
        if (input == null || input.kind() == null || input.name() == null
                || input.name().isBlank() || input.name().trim().length() > 80) {
            throw bad("Each element needs a type and a name of 1–80 characters");
        }
        String kind = input.kind();
        if (!kind.equals("ROOM") && !kind.equals("FURNITURE") && !kind.equals("OPENING")) {
            throw bad("Unsupported design element type");
        }
        int x = integer(input.xCm(), 0, floorW, "X coordinate");
        int y = integer(input.yCm(), 0, floorH, "Y coordinate");
        int w = integer(input.widthCm(), 20, floorW, "Element width");
        int h = integer(input.heightCm(), 20, floorH, "Element height");
        // Using subtraction avoids overflow from attacker-supplied coordinates.
        if (x > floorW - w || y > floorH - h) throw bad("Element must fit inside the floor boundary");
        int rotation = integer(input.rotation(), 0, 359, "Rotation");
        if (kind.equals("ROOM") && rotation != 0) throw bad("Rooms must remain axis-aligned");
        // A rotated rectangle must also fit: checking the unrotated bounds alone
        // would allow beds and openings to extend past the measured floor.
        double radians = Math.toRadians(rotation);
        double rotatedWidth = Math.abs(w * Math.cos(radians)) + Math.abs(h * Math.sin(radians));
        double rotatedHeight = Math.abs(w * Math.sin(radians)) + Math.abs(h * Math.cos(radians));
        double centerX = x + w / 2.0;
        double centerY = y + h / 2.0;
        if (centerX - rotatedWidth / 2 < -0.001 || centerY - rotatedHeight / 2 < -0.001
                || centerX + rotatedWidth / 2 > floorW + 0.001
                || centerY + rotatedHeight / 2 > floorH + 0.001) {
            throw bad("Rotated element must remain inside the floor boundary");
        }
        return new DesignElement(kind, input.name().trim(), x, y, w, h, rotation, order);
    }

    public static int roomOverlaps(List<DesignElement> elements) {
        int count = 0;
        for (int i = 0; i < elements.size(); i++) {
            DesignElement a = elements.get(i);
            if (!a.getKind().equals("ROOM")) continue;
            for (int j = i + 1; j < elements.size(); j++) {
                DesignElement b = elements.get(j);
                if (!b.getKind().equals("ROOM")) continue;
                if (Math.min(a.getXCm() + a.getWidthCm(), b.getXCm() + b.getWidthCm())
                        > Math.max(a.getXCm(), b.getXCm())
                        && Math.min(a.getYCm() + a.getHeightCm(), b.getYCm() + b.getHeightCm())
                        > Math.max(a.getYCm(), b.getYCm())) count++;
            }
        }
        return count;
    }

    private static int integer(Integer value, int min, int max, String name) {
        if (value == null || value < min || value > max) throw bad(name + " is outside valid limits");
        return value;
    }
    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
