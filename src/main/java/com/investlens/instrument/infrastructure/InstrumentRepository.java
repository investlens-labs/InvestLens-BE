package com.investlens.instrument.infrastructure;

import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentType;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
            order by i.ticker asc
            """)
    List<Instrument> search(@Param("query") String query, @Param("type") InstrumentType type);
}
