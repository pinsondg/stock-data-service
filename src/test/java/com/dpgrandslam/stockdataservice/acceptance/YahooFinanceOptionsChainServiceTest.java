package com.dpgrandslam.stockdataservice.acceptance;

import com.dpgrandslam.stockdataservice.domain.error.OptionsChainLoadException;
import com.dpgrandslam.stockdataservice.domain.model.options.OptionsChain;
import com.dpgrandslam.stockdataservice.domain.service.YahooFinanceOptionsChainLoadService;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertNotNull;

@Ignore
public class YahooFinanceOptionsChainServiceTest extends AcceptanceTest {

    @Autowired
    private YahooFinanceOptionsChainLoadService subject;

    @Test
    public void testGetCompleteOptionsChain() throws OptionsChainLoadException {
        List<OptionsChain> chains = subject.loadFullLiveOptionsChain("AAPL");
        List<OptionsChain> chains2 = subject.loadFullLiveOptionsChain("AAL");
        List<OptionsChain> chains3 = subject.loadFullLiveOptionsChain("SPY");
        assertNotNull(chains);
    }

    @Test
    public void basicLoadTest() throws OptionsChainLoadException {
        String ticker = "SPY";
        OptionsChain chain = subject.loadLiveOptionsChainForClosestExpiration(ticker);
        int callsMade = 0;
        while (chain != null) {
            chain = subject.loadLiveOptionsChainForClosestExpiration(ticker);
            callsMade++;
            System.out.println("\rCalls made: " + callsMade);
        }
    }
}
