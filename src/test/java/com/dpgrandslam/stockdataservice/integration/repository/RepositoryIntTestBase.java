package com.dpgrandslam.stockdataservice.integration.repository;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@Profile({"local"})
@SpringBootTest
@RunWith(SpringRunner.class)
@Ignore
@Transactional
public class RepositoryIntTestBase {


}
