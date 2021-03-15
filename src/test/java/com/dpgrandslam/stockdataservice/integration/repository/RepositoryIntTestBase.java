package com.dpgrandslam.stockdataservice.integration.repository;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit4.SpringRunner;

@Profile({"local"})
@DataJpaTest
@RunWith(SpringRunner.class)
@Ignore
public class RepositoryIntTestBase {


}
