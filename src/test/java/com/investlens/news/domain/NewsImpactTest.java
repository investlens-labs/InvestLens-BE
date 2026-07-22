package com.investlens.news.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.investlens.instrument.domain.Instrument;
import com.investlens.instrument.domain.InstrumentType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NewsImpactTest {
    private final Instrument instrument = new Instrument("NVDA", "NVIDIA", InstrumentType.STOCK);

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
    void acceptsScoreFromOneToFive(int score) {
        new NewsImpact(instrument, ImpactDirection.POSITIVE, score, "관련 수요 증가 가능성");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 11})
    void rejectsOutOfRangeScore(int score) {
        assertThatThrownBy(() -> new NewsImpact(instrument, ImpactDirection.NEGATIVE, score, "영향"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {99, 101})
    void rejectsProbabilitiesThatDoNotSumToOneHundred(int total) {
        assertThatThrownBy(() -> new NewsImpact(instrument, ImpactDirection.POSITIVE, 3, "영향",
                total - 40, 20, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 50, 100})
    void acceptsProbabilitiesThatSumToOneHundred(int upProbability) {
        var impact = new NewsImpact(instrument, ImpactDirection.POSITIVE, 3, "영향",
                upProbability, 0, 100 - upProbability);

        assertThat(impact.getUpProbability()).isEqualTo(upProbability);
    }
}
