package com.investlens.instrument.application;

import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import com.investlens.instrument.domain.InstrumentType;
import com.investlens.instrument.infrastructure.InstrumentRepository;
import com.investlens.instrument.presentation.dto.InstrumentResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InstrumentQueryService {
    private final InstrumentRepository instrumentRepository;

    public InstrumentQueryService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    public List<InstrumentResponse> search(String query, InstrumentType type) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return instrumentRepository.search(normalizedQuery, type).stream()
                .map(InstrumentResponse::from)
                .toList();
    }

    public InstrumentResponse get(UUID instrumentId) {
        return instrumentRepository.findById(instrumentId)
                .map(InstrumentResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUMENT_NOT_FOUND));
    }
}
