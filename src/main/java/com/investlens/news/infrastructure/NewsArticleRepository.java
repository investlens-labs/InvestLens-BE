package com.investlens.news.infrastructure;

import com.investlens.news.domain.NewsArticle;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {
    Optional<NewsArticle> findByCanonicalUrl(String canonicalUrl);

    @Query("select n from NewsArticle n " +
            "where exists (select r.id from NewsRelatedInstrument r where r.news = n and r.instrument.id in :instrumentIds) " +
            "and ((:direction is null and :minScore is null) or exists (select i.id from NewsImpact i " +
            "where i.news = n and i.instrument.id in :instrumentIds " +
            "and (:direction is null or i.direction = :direction) and (:minScore is null or i.score >= :minScore)))")
    Page<NewsArticle> findFeed(@Param("instrumentIds") Collection<UUID> instrumentIds,
                               @Param("direction") com.investlens.news.domain.ImpactDirection direction,
                               @Param("minScore") Integer minScore,
                               Pageable pageable);

    @Query("select n from NewsArticle n " +
            "where exists (select r.id from NewsRelatedInstrument r " +
            "where r.news = n and r.instrument.id = :instrumentId)")
    Page<NewsArticle> findByInstrumentId(@Param("instrumentId") UUID instrumentId, Pageable pageable);

    @EntityGraph(attributePaths = {"impacts", "impacts.instrument"})
    @Query("select distinct n from NewsArticle n join n.relatedInstruments r where n.id = :id and r.instrument.id in :instrumentIds")
    Optional<NewsArticle> findDetailForInstruments(@Param("id") UUID id,
                                                    @Param("instrumentIds") Collection<UUID> instrumentIds);

    @EntityGraph(attributePaths = {"relatedInstruments", "relatedInstruments.instrument"})
    List<NewsArticle> findTop50ByAnalysisStatusInOrderByUpdatedAtAsc(
            Collection<com.investlens.news.domain.AnalysisStatus> statuses);
}
