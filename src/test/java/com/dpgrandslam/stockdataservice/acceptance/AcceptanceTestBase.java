package com.dpgrandslam.stockdataservice.acceptance;

import com.dpgrandslam.stockdataservice.StockDataServiceApplication;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@ActiveProfiles({"local"})
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = StockDataServiceApplication.class)
@Ignore
public class AcceptanceTestBase {


}
