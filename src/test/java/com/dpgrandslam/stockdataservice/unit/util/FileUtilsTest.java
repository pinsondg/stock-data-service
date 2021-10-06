package com.dpgrandslam.stockdataservice.unit.util;

import com.dpgrandslam.stockdataservice.domain.util.FileUtils;
import org.junit.Test;

import java.io.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class FileUtilsTest {

    @Test
    public void testGetResourceFile() throws IOException {
        File file = FileUtils.getResourceFile("test.txt");
        assertNotNull(file);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        assertEquals("Hello World", reader.readLine());
        reader.close();
    }

    @Test(expected = FileNotFoundException.class)
    public void testGetResourceFile_doesNotExist() throws IOException {
        FileUtils.getResourceFile("test_1.txt");
    }
}
