package com.dpgrandslam.stockdataservice.testUtils;

import com.dpgrandslam.stockdataservice.domain.model.options.HistoricalOption;
import com.dpgrandslam.stockdataservice.domain.model.options.Option;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionPriceData;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;

public class TestDataFactory {

    public static class HistoricalOptionMother {

        public static HistoricalOption.HistoricalOptionBuilder completeWithOnePriceData() {
            return HistoricalOption.builder()
                    .optionType(Option.OptionType.CALL)
                    .expiration(LocalDate.now(ZoneId.of("America/New_York")))
                    .ticker("TEST")
                    .strike(12.5)
                    .historicalPriceData(Collections.singleton(OptionPriceDataMother
                            .complete()
                            .build())
                    );
        }

        public static HistoricalOption.HistoricalOptionBuilder noPriceData() {
            return completeWithOnePriceData().historicalPriceData(null);
        }
    }

    public static class OptionPriceDataMother {
        public static OptionPriceData.OptionPriceDataBuilder complete() {
           return OptionPriceData.builder()
                    .dataObtainedDate(Timestamp.from(Instant.now()))
                    .bid(12.0)
                    .ask(11.0)
                    .marketPrice(11.5)
                    .impliedVolatility(100.0)
                    .openInterest(120)
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
}
