package com.investlens.portfolio.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import com.investlens.instrument.infrastructure.InstrumentRepository;
import com.investlens.portfolio.infrastructure.PortfolioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {
    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        portfolioService = new PortfolioService(portfolioRepository, instrumentRepository);
    }

    @Test
    void addRejectsDuplicatedInstrument() {
        UUID userId = UUID.randomUUID();
        UUID instrumentId = UUID.randomUUID();
        when(portfolioRepository.existsByUserIdAndInstrument_Id(userId, instrumentId)).thenReturn(true);

        assertThatThrownBy(() -> portfolioService.add(userId, instrumentId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PORTFOLIO_DUPLICATED);

        verify(instrumentRepository, never()).findById(instrumentId);
    }

    @Test
    void deleteReturnsNotFoundWhenItemDoesNotBelongToUser() {
        UUID userId = UUID.randomUUID();
        UUID portfolioItemId = UUID.randomUUID();
        when(portfolioRepository.findByIdAndUserId(portfolioItemId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.delete(userId, portfolioItemId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PORTFOLIO_NOT_FOUND);

        verify(portfolioRepository, never()).deleteById(portfolioItemId);
    }
}
