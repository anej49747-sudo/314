package csit314.renovation.dispute.boundary;

import csit314.renovation.dispute.control.DuplicateOpenReportException;
import csit314.renovation.dispute.control.ReportAlreadyClosedException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = { DesignerReportController.class, AdminDesignerReportController.class })
public class DesignerReportExceptionHandler {
    @ExceptionHandler(DuplicateOpenReportException.class)
    ResponseEntity<Map<String, String>> duplicate() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "You already have an open report for this designer."));
    }

    @ExceptionHandler(ReportAlreadyClosedException.class)
    ResponseEntity<Map<String, String>> closed() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "This report has already been closed. Refresh the list."));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<Map<String, String>> invalid(Exception ignored) {
        return ResponseEntity.badRequest().body(Map.of("message", "Please check the report details."));
    }
}
