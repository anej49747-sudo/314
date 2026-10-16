package csit314.renovation.account.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Salutation {
    MR("Mr"), MRS("Mrs"), MS("Ms"), MDM("Mdm"), MX("Mx"), DR("Dr"), PROF("Prof");

    private final String label;

    Salutation(String label) {
        this.label = label;
    }

    @JsonValue
    public String label() {
        return label;
    }

    @JsonCreator
    public static Salutation fromLabel(String label) {
        if (label == null) {
            return null;
        }
        for (Salutation value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported salutation");
    }
}
