package csit314.renovation.moodboard.boundary;

import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Return a conflict instead of silently overwriting a board from another session. */
@RestControllerAdvice(assignableTypes = CustomerMoodboardController.class)
public class MoodboardExceptionHandler {
    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> stale() {
        return Map.of("message", "Moodboard changed in another session. Reload before saving.");
    }
}
