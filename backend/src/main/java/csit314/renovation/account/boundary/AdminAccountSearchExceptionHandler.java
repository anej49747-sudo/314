package csit314.renovation.account.boundary;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminAccountSearchController.class)
public class AdminAccountSearchExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidFilter(IllegalArgumentException ignored) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid account search parameters"));
    }
}
