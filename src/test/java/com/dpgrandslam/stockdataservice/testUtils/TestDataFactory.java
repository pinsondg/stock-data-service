package com.dpgrandslam.stockdataservice.testUtils;

import com.dpgrandslam.stockdataservice.domain.model.OptionPriceDataLoadRetry;
import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestDataFactory {

    public static class HistoricalOptionMother {

        public static HistoricalOption.HistoricalOptionBuilder completeWithOnePriceData() {
            Set<OptionPriceData> optionPriceDataSet = new HashSet<>();
            optionPriceDataSet.add(OptionPriceDataMother
                    .complete()
                    .build());
            return HistoricalOption.builder()
                    .optionType(Option.OptionType.CALL)
                    .expiration(LocalDate.now(ZoneId.of("America/New_York")))
                    .ticker("TEST")
                    .strike(12.5)
                    .historicalPriceData(optionPriceDataSet);
        }

        public static HistoricalOption.HistoricalOptionBuilder noPriceData() {
            return completeWithOnePriceData().historicalPriceData(null);
        }
    }

    public static class OptionPriceDataMother {

        private static LocalDate tradeDate = LocalDate.now();

        public static OptionPriceData.OptionPriceDataBuilder complete() {
            tradeDate = tradeDate.minusDays(1);
           return OptionPriceData.builder()
                    .dataObtainedDate(Timestamp.from(Instant.now()))
                    .bid(12.0)
                    .ask(11.0)
                    .lastTradePrice(11.5)
                    .impliedVolatility(100.0)
                    .openInterest(120)
                   .tradeDate(tradeDate)
                    .volume(30);
        }
    }

    public static class OptionsChainMother {

        public static OptionsChain oneOption() {
            OptionsChain chain = OptionsChain.builder()
                    .expirationDate(LocalDate.now(ZoneId.of("America/New_York")))
                    .ticker("TEST")
                    .build();
            chain.addOption(HistoricalOptionMother.completeWithOnePriceData().build());
            return chain;
        }

        public static OptionsChain.OptionsChainBuilder emptyOptions() {
            return OptionsChain.builder()
                    .ticker("TEST")
                    .expirationDate(LocalDate.now(ZoneId.of("America/New_York")));
        }
    }

    public static class OptionPriceDataLoadRetryMother {

        public static OptionPriceDataLoadRetry.OptionPriceDataLoadRetryBuilder complete() {
           return OptionPriceDataLoadRetry.builder()
                    .optionExpiration(LocalDate.now())
                    .tradeDate(LocalDate.now())
                    .optionTicker("TEST")
                    .retryCount(0);
        }
    }
}
