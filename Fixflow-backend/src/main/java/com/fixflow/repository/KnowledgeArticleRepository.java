package com.fixflow.repository;

import com.fixflow.entity.KnowledgeArticle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, String> {

    List<KnowledgeArticle> findAllByOrderByTitleAsc();

    @Query(value = """
            SELECT id, title, category, keywords, body, common_causes, diagnostic_steps,
                   verification_steps, suggested_fix, source_label
            FROM knowledge_articles
            WHERE search_vector @@ websearch_to_tsquery('english', :query)
            ORDER BY ts_rank(search_vector, websearch_to_tsquery('english', :query)) DESC, title
            LIMIT :limit
            """, nativeQuery = true)
    List<KnowledgeArticle> searchByText(@Param("query") String query, @Param("limit") int limit);
}