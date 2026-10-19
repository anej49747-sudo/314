package csit314.renovation.category.boundary;

import csit314.renovation.category.control.DuplicateCategoryException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CategoryController.class)
public class CategoryExceptionHandler {
    public record ErrorResponse(String message, Map<String, String> fieldErrors) {}

    @ExceptionHandler(DuplicateCategoryException.class)
    public ResponseEntity<ErrorResponse> duplicate(DuplicateCategoryException ignored) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "A category with this name already exists", Map.of("name", "Category name already exists")));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException error) {
        Map<String, String> fields = new LinkedHashMap<>();
        error.getBindingResult().getFieldErrors().forEach(field ->
                fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorResponse("Please correct the highlighted fields", fields));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> invalidBody(Exception error) {
        String message = error instanceof IllegalArgumentException ? error.getMessage() : "Invalid category request";
        return ResponseEntity.badRequest().body(new ErrorResponse(message, Map.of()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> unauthenticated(AuthenticationException ignored) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Please sign in again", Map.of()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(AccessDeniedException ignored) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Platform Manager access required", Map.of()));
    }
}
