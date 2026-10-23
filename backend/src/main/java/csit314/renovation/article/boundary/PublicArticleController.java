package csit314.renovation.article.boundary;

import csit314.renovation.article.control.ArticleService;
import csit314.renovation.article.dto.ArticleResponse;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
public class PublicArticleController {
    private final ArticleService service;

    public PublicArticleController(ArticleService service) { this.service = service; }

    @GetMapping
    public List<ArticleResponse> list() { return service.list(); }

    @GetMapping("/{articleId}")
    public ArticleResponse get(@PathVariable Long articleId) { return service.get(articleId); }

    @GetMapping("/{articleId}/cover")
    public ResponseEntity<byte[]> cover(@PathVariable Long articleId) {
        ArticleService.ImageContent image = service.cover(articleId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(image.contentType())).body(image.data());
    }
}
