package com.dpgrandslam.stockdataservice.domain.util;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.util.Objects;

public class FileUtils {

    public static File getResourceFile(String fileName) throws IOException {
        try {
            return new File(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource(fileName)).getFile());
        } catch (NullPointerException e) {
            InputStream stream = new ClassPathResource(fileName).getInputStream();
            String suffix = fileName.substring(fileName.lastIndexOf('.') + 1);
            String prefix = fileName.substring(0, fileName.lastIndexOf('.'));
            File file = File.createTempFile(prefix, suffix);
            file.deleteOnExit();
            OutputStream out = new FileOutputStream(file);
            IOUtils.copy(stream, out);
            return file;
        }
    }

}
