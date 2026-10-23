package csit314.renovation.article.repository;

import csit314.renovation.article.entity.RenovationArticle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RenovationArticleRepository extends JpaRepository<RenovationArticle, Long> {
    List<RenovationArticle> findAllByOrderByPublishedAtDescIdDesc();
}
