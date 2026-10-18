package csit314.renovation.designer.control;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class InitialPasswordGenerator {
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%*-_+?";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;
    private static final int LENGTH = 24;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        char[] password = new char[LENGTH];
        password[0] = pick(UPPER);
        password[1] = pick(LOWER);
        password[2] = pick(DIGITS);
        password[3] = pick(SYMBOLS);
        for (int index = 4; index < LENGTH; index++) {
            password[index] = pick(ALL);
        }
        // Shuffle the guaranteed character types so the first four positions are not predictable.
        for (int index = LENGTH - 1; index > 0; index--) {
            int swapIndex = random.nextInt(index + 1);
            char current = password[index];
            password[index] = password[swapIndex];
            password[swapIndex] = current;
        }
        return new String(password);
    }

    private char pick(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }
}
