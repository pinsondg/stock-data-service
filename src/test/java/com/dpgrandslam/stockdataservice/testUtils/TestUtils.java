package com.dpgrandslam.stockdataservice.testUtils;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TestUtils {

    public static File loadResourceFile(String path) throws IOException {
        return new ClassPathResource(path).getFile();
    }

    public static byte[] loadBodyFromTestResourceFile(String path) throws IOException {
        File resource = new ClassPathResource(path).getFile();
        return Files.readAllBytes(resource.toPath());
    }
}
