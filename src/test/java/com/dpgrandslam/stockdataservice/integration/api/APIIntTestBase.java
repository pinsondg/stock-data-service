package com.dpgrandslam.stockdataservice.integration.api;

import com.dpgrandslam.stockdataservice.StockDataServiceApplication;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockserver.junit.MockServerRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StockDataServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "local-mock"})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Ignore
public class APIIntTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this,true, 1080);

}
