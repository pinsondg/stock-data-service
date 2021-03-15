package com.dpgrandslam.stockdataservice.testUtils;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

public class TestUtils {

    public static File loadResourceFile(String path) throws IOException {
        return new ClassPathResource(path).getFile();
    }
}
