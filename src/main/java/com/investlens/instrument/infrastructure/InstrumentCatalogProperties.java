package com.investlens.instrument.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.instrument-catalog")
public record InstrumentCatalogProperties(
        boolean enabled,
        String cron,
        Duration timeout,
        String nasdaqListedUrl,
        String otherUsListedUrl,
        String krxListedUrl,
        String krEtfUrl
) {
    public InstrumentCatalogProperties {
        if (cron == null || cron.isBlank()) cron = "0 0 3 * * *";
        if (timeout == null) timeout = Duration.ofSeconds(20);
        if (nasdaqListedUrl == null || nasdaqListedUrl.isBlank()) {
            nasdaqListedUrl = "https://www.nasdaqtrader.com/dynamic/SymDir/nasdaqlisted.txt";
        }
        if (otherUsListedUrl == null || otherUsListedUrl.isBlank()) {
            otherUsListedUrl = "https://www.nasdaqtrader.com/dynamic/SymDir/otherlisted.txt";
        }
        if (krxListedUrl == null || krxListedUrl.isBlank()) {
            krxListedUrl = "https://kind.krx.co.kr/corpgeneral/corpList.do?method=download&searchType=13";
        }
        if (krEtfUrl == null || krEtfUrl.isBlank()) {
            krEtfUrl = "https://finance.naver.com/api/sise/etfItemList.nhn";
        }
    }
}
