package com.investlens.portfolio.application;

import com.investlens.portfolio.infrastructure.PortfolioRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PortfolioInstrumentQueryService {
    private final PortfolioRepository portfolioRepository;

    public PortfolioInstrumentQueryService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    public List<UUID> findInstrumentIds(UUID userId) {
        return portfolioRepository.findInstrumentIdsByUserId(userId);
    }
}
