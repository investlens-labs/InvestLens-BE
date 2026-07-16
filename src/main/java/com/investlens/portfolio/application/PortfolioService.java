package com.investlens.portfolio.application;

import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.infrastructure.InstrumentRepository;
import com.investlens.portfolio.domain.PortfolioItem;
import com.investlens.portfolio.infrastructure.PortfolioRepository;
import com.investlens.portfolio.presentation.dto.PortfolioItemResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final InstrumentRepository instrumentRepository;

    public PortfolioService(PortfolioRepository portfolioRepository, InstrumentRepository instrumentRepository) {
        this.portfolioRepository = portfolioRepository;
        this.instrumentRepository = instrumentRepository;
    }

    @Transactional(readOnly = true)
    public List<PortfolioItemResponse> getPortfolio(UUID userId) {
        return portfolioRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PortfolioItemResponse::from)
                .toList();
    }

    @Transactional
    public PortfolioItemResponse add(UUID userId, UUID instrumentId) {
        if (portfolioRepository.existsByUserIdAndInstrument_Id(userId, instrumentId)) {
            throw new BusinessException(ErrorCode.PORTFOLIO_DUPLICATED);
        }

        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUMENT_NOT_FOUND));

        try {
            PortfolioItem item = portfolioRepository.saveAndFlush(new PortfolioItem(userId, instrument));
            return PortfolioItemResponse.from(item);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.PORTFOLIO_DUPLICATED);
        }
    }

    @Transactional
    public void delete(UUID userId, UUID portfolioItemId) {
        PortfolioItem item = portfolioRepository.findByIdAndUserId(portfolioItemId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));
        portfolioRepository.delete(item);
    }
}
