package csit314.renovation.recognition.boundary;

import csit314.renovation.recognition.control.RecognitionService;
import csit314.renovation.recognition.dto.PublicRecognitionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicRecognitionController {
    private final RecognitionService service;

    public PublicRecognitionController(RecognitionService service) { this.service = service; }

    @GetMapping("/api/recognitions")
    public PublicRecognitionResponse list() { return service.publicRecognition(); }
}
