package com.investlens.news.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NewsImpactTest {
    private final Instrument instrument = new Instrument("NVDA", "NVIDIA", InstrumentType.STOCK);

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void acceptsScoreFromOneToFive(int score) {
        new NewsImpact(instrument, ImpactDirection.POSITIVE, score, "관련 수요 증가 가능성");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 6})
    void rejectsOutOfRangeScore(int score) {
        assertThatThrownBy(() -> new NewsImpact(instrument, ImpactDirection.NEGATIVE, score, "영향"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
