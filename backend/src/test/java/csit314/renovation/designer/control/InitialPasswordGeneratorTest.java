package csit314.renovation.designer.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import csit314.renovation.registration.validation.RegistrationPasswordValidator;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InitialPasswordGeneratorTest {
    @Test
    void createsUnpredictablePasswordsThatPassTheAgreedPolicy() {
        InitialPasswordGenerator generator = new InitialPasswordGenerator();
        RegistrationPasswordValidator validator = new RegistrationPasswordValidator();
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String password = generator.generate();
            assertEquals(24, password.length());
            assertTrue(validator.isValid(password, null));
            generated.add(password);
        }
        assertEquals(100, generated.size());
    }
}
