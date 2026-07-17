package com.investlens.news.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.investlens.common.error.BusinessException;
import org.junit.jupiter.api.Test;

class NewsLanguageTest {
    @Test
    void resolvesSupportedLanguageCodeCaseInsensitively() {
        assertThat(NewsLanguage.fromCode(" KO ")).isEqualTo(NewsLanguage.KO);
        assertThat(NewsLanguage.fromCode("en")).isEqualTo(NewsLanguage.EN);
    }

    @Test
    void rejectsUnsupportedLanguage() {
        assertThatThrownBy(() -> NewsLanguage.fromCode("fr"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ko, en, ja, zh");
    }
}
