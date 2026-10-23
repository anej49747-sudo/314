package csit314.renovation.article.control;

import csit314.renovation.article.dto.ArticleResponse;
import csit314.renovation.article.dto.CreateArticleRequest;
import csit314.renovation.article.entity.RenovationArticle;
import csit314.renovation.article.repository.RenovationArticleRepository;
import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.portfolio.control.ProjectImageStorage;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArticleService {
    private final RenovationArticleRepository articles;
    private final AuthenticationService authenticationService;
    private final ProjectImageStorage storage;

    public ArticleService(RenovationArticleRepository articles,
            AuthenticationService authenticationService, ProjectImageStorage storage) {
        this.articles = articles;
        this.authenticationService = authenticationService;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> list() {
        return articles.findAllByOrderByPublishedAtDescIdDesc().stream()
                .map(ArticleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ArticleResponse get(Long id) {
        return ArticleResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public ImageContent cover(Long id) {
        RenovationArticle article = find(id);
        if (article.getCoverStorageKey() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cover photo not found");
        }
        return new ImageContent(storage.read(article.getCoverStorageKey()), article.getCoverContentType());
    }

    @Transactional
    public ArticleResponse publish(CreateArticleRequest request, MultipartFile cover, Authentication authentication) {
        authenticationService.currentManager(authentication);
        // Validate all input before writing files or database records.
        String title = normalized(request.title(), 5, 120, "Article title");
        String summary = normalized(request.summary(), 20, 240, "Summary");
        String content = normalized(request.content(), 100, 10000, "Article content");
        if (cover != null && cover.isEmpty()) {
            throw new IllegalArgumentException("Select a cover photo or publish without one");
        }
        ProjectImageStorage.ValidatedImage image = cover == null ? null : storage.validate(cover);
        List<String> createdKeys = new ArrayList<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) createdKeys.forEach(storage::deleteQuietly);
            }
        });
        ProjectImageStorage.StoredImage stored = image == null ? null : storage.store(image);
        if (stored != null) createdKeys.add(stored.key());
        RenovationArticle article = articles.saveAndFlush(new RenovationArticle(title, summary, content,
                stored == null ? null : stored.key(), stored == null ? null : stored.contentType()));
        return ArticleResponse.from(article);
    }

    private RenovationArticle find(Long id) {
        return articles.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    private static String normalized(String value, int min, int max, String label) {
        if (value == null) throw new IllegalArgumentException(label + " is required");
        String clean = value.strip();
        if (clean.length() < min || clean.length() > max) {
            throw new IllegalArgumentException(label + " must be between " + min + " and " + max + " characters");
        }
        return clean;
    }

    public record ImageContent(byte[] data, String contentType) { }
}
