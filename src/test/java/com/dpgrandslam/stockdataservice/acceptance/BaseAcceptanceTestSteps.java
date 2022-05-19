package com.dpgrandslam.stockdataservice.acceptance;

import com.amazonaws.services.s3.AmazonS3;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


@RunWith(SpringRunner.class)
@SpringBootTest
@CucumberContextConfiguration
@ActiveProfiles({"local", "local-mock", "test"})
@AutoConfigureMockMvc
@Ignore
public class BaseAcceptanceTestSteps {

    @Autowired
    protected MockMvc mockMvc;

}
