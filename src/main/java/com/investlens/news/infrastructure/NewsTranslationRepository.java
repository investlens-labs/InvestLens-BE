package com.investlens.news.infrastructure;

import com.investlens.news.domain.NewsTranslation;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsTranslationRepository extends JpaRepository<NewsTranslation, UUID> {
    @EntityGraph(attributePaths = "news")
    @Query("select t from NewsTranslation t where t.news.id in :newsIds and t.language = :language")
    List<NewsTranslation> findAllByNewsIdInAndLanguage(@Param("newsIds") Collection<UUID> newsIds,
                                                       @Param("language") String language);
}
