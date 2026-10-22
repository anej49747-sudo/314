package csit314.renovation.account.boundary;

import csit314.renovation.account.control.AccountNotFoundException;
import csit314.renovation.account.control.AccountStatusConflictException;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        AdminAccountModerationController.class, AdminLoginHistoryController.class})
public class AdminAccountManagementExceptionHandler {
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Account not found"));
    }

    @ExceptionHandler(AccountStatusConflictException.class)
    public ResponseEntity<Map<String, String>> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Account status changed. Refresh and try again."));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class, ConstraintViolationException.class})
    public ResponseEntity<Map<String, String>> invalidRequest(Exception ignored) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid account management request"));
    }
}
