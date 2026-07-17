package com.investlens.news.domain;

import com.investlens.common.error.BusinessException;
import com.investlens.common.error.ErrorCode;
import java.util.Arrays;

public enum NewsLanguage {
    KO("ko", "한국어"),
    EN("en", "English"),
    JA("ja", "日本語"),
    ZH("zh", "简体中文");

    private final String code;
    private final String displayName;

    NewsLanguage(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public static NewsLanguage fromCode(String value) {
        return Arrays.stream(values())
                .filter(language -> language.code.equalsIgnoreCase(value == null ? "" : value.strip()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_REQUEST, "language은 ko, en, ja, zh 중 하나여야 합니다."));
    }
}
