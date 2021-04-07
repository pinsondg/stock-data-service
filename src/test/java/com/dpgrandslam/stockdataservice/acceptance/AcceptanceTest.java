package com.dpgrandslam.stockdataservice.acceptance;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.mockserver.junit.MockServerRule;

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/test/resources")
public class AcceptanceTest {

    @ClassRule
    public static MockServerRule mockServerRule = new MockServerRule(AcceptanceTest.class, 1080);

}
