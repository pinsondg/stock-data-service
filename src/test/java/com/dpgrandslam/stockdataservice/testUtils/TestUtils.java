package com.dpgrandslam.stockdataservice.testUtils;

import com.dpgrandslam.stockdataservice.domain.util.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class TestUtils {

    public static File loadResourceFile(String path) throws IOException {
        return new ClassPathResource(path).getFile();
    }

    public static byte[] loadBodyFromTestResourceFile(String path) throws IOException {
        return Files.readAllBytes(FileUtils.getResourceFile(path).toPath());
    }

    public static String loadHtmlFileAndClean(String path) throws IOException {
        File file = FileUtils.getResourceFile(path);

        Document document = Jsoup.parse(file, "UTF-8");
        document.select("style").forEach(Node::remove);
        document.select("meta").forEach(Node::remove);
        document.select("script").forEach(Node::remove);
        document.select("head").forEach(Node::remove);
        document.select("nav").forEach(Node::remove);

        return document.toString();
    }
}
