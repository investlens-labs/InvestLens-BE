package com.investlens.instrument.application;

import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentType;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.infrastructure.InstrumentRepository;
import com.investlens.instrument.presentation.dto.InstrumentResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InstrumentQueryService {
    private final InstrumentRepository instrumentRepository;
    private final InstrumentLogoUrlProvider instrumentLogoUrlProvider;

    public InstrumentQueryService(InstrumentRepository instrumentRepository,
                                  InstrumentLogoUrlProvider instrumentLogoUrlProvider) {
        this.instrumentRepository = instrumentRepository;
        this.instrumentLogoUrlProvider = instrumentLogoUrlProvider;
    }

    public List<InstrumentResponse> search(String query, InstrumentType type, InstrumentMarket market, int limit) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return instrumentRepository.search(normalizedQuery, type, market, PageRequest.of(0, limit)).stream()
                .map(this::toResponse)
                .toList();
    }

    public InstrumentResponse get(UUID instrumentId) {
        return instrumentRepository.findById(instrumentId)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUMENT_NOT_FOUND));
    }

    private InstrumentResponse toResponse(Instrument instrument) {
        return InstrumentResponse.from(
                instrument,
                instrumentLogoUrlProvider.get(instrument),
                InstrumentLogoUrlProvider.ATTRIBUTION_URL
        );
    }
}
