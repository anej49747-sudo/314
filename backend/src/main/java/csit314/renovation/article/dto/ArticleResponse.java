package csit314.renovation.article.dto;

import csit314.renovation.article.entity.RenovationArticle;
import java.time.Instant;

public record ArticleResponse(Long id, String title, String summary, String content,
        Instant publishedAt, String coverUrl) {
    public static ArticleResponse from(RenovationArticle article) {
        return new ArticleResponse(article.getId(), article.getTitle(), article.getSummary(),
                article.getContent(), article.getPublishedAt(), article.getCoverStorageKey() == null
                        ? null : "/api/articles/" + article.getId() + "/cover");
    }
}
