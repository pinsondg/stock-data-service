package com.dpgrandslam.stockdataservice.integration.client;


import com.dpgrandslam.stockdataservice.StockDataServiceApplication;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockserver.junit.MockServerRule;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@ActiveProfiles({"local", "local-mock"})
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = StockDataServiceApplication.class)
@Ignore
public class MockClientTest {

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, 1080);

    protected byte[] loadBodyFromTestResourceFile(String path) throws IOException {
        File resource = new ClassPathResource(path).getFile();
        return Files.readAllBytes(resource.toPath());
    }

}
