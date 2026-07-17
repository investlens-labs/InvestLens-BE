package com.investlens.instrument.infrastructure;

import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.domain.InstrumentType;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {
    Optional<Instrument> findByTicker(String ticker);
    @Query("""
            select i from Instrument i
            where (:query is null
                or lower(i.ticker) like lower(concat('%', :query, '%'))
                or lower(i.companyName) like lower(concat('%', :query, '%')))
              and (:type is null or i.type = :type)
              and (:market is null or i.market = :market)
              and i.active = true
            order by
              case when :query is not null and lower(i.ticker) = lower(:query) then 0 else 1 end,
              i.ticker asc
            """)
    List<Instrument> search(@Param("query") String query, @Param("type") InstrumentType type,
                            @Param("market") InstrumentMarket market, Pageable pageable);
}
