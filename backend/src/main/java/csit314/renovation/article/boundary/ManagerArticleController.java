package csit314.renovation.article.boundary;

import csit314.renovation.article.control.ArticleService;
import csit314.renovation.article.dto.ArticleResponse;
import csit314.renovation.article.dto.CreateArticleRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/manager/articles")
public class ManagerArticleController {
    private final ArticleService service;

    public ManagerArticleController(ArticleService service) { this.service = service; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse publish(@Valid @RequestPart("article") CreateArticleRequest article,
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            Authentication authentication) {
        return service.publish(article, cover, authentication);
    }
}
