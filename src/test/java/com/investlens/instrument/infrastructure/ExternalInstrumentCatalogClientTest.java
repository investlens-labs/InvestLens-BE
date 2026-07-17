package com.investlens.instrument.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investlens.instrument.domain.InstrumentMarket;
import com.investlens.instrument.domain.InstrumentType;
import org.junit.jupiter.api.Test;

class ExternalInstrumentCatalogClientTest {
    @Test
    void parsesNasdaqStocksAndEtfsAndExcludesTestIssues() {
        String body = """
                Symbol|Security Name|Market Category|Test Issue|Financial Status|Round Lot Size|ETF|NextShares
                AAPL|Apple Inc. - Common Stock|Q|N|N|100|N|N
                QQQ|Invesco QQQ Trust|G|N|N|100|Y|N
                ZTEST|Test Security|Q|Y|N|100|N|N
                File Creation Time: 0716202621:32|||||||
                """;

        var result = ExternalInstrumentCatalogClient.parseNasdaqDirectory(body, true);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).ticker()).isEqualTo("AAPL");
        assertThat(result.get(0).type()).isEqualTo(InstrumentType.STOCK);
        assertThat(result.get(0).market()).isEqualTo(InstrumentMarket.US);
        assertThat(result.get(1).type()).isEqualTo(InstrumentType.ETF);
    }

    @Test
    void parsesKoreanStocksAndEtfs() {
        String stockHtml = """
                <table><tr><th>회사명</th><th>시장구분</th><th>종목코드</th></tr>
                <tr><td>삼성전자</td><td>유가증권</td><td style="text-align:center">005930</td></tr></table>
                """;
        String etfJson = """
                {"result":{"etfItemList":[{"itemcode":"069500","itemname":"KODEX 200"}]}}
                """;

        var stocks = ExternalInstrumentCatalogClient.parseKrxStocks(stockHtml);
        var etfs = ExternalInstrumentCatalogClient.parseKoreanEtfs(etfJson, new ObjectMapper());

        assertThat(stocks).singleElement().satisfies(item -> {
            assertThat(item.ticker()).isEqualTo("005930");
            assertThat(item.companyName()).isEqualTo("삼성전자");
            assertThat(item.type()).isEqualTo(InstrumentType.STOCK);
            assertThat(item.market()).isEqualTo(InstrumentMarket.KR);
        });
        assertThat(etfs).singleElement().satisfies(item -> {
            assertThat(item.ticker()).isEqualTo("069500");
            assertThat(item.type()).isEqualTo(InstrumentType.ETF);
            assertThat(item.market()).isEqualTo(InstrumentMarket.KR);
        });
    }
}
