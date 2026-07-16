package com.investlens.portfolio.infrastructure;

import com.investlens.portfolio.domain.PortfolioItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PortfolioRepository extends JpaRepository<PortfolioItem, UUID> {
    List<PortfolioItem> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndInstrument_Id(UUID userId, UUID instrumentId);

    Optional<PortfolioItem> findByIdAndUserId(UUID id, UUID userId);

    @Query("select p.instrument.id from PortfolioItem p where p.userId = :userId")
    List<UUID> findInstrumentIdsByUserId(@Param("userId") UUID userId);
}
