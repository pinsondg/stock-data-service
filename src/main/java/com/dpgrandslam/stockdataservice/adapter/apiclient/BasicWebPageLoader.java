package com.dpgrandslam.stockdataservice.adapter.apiclient;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class BasicWebPageLoader implements WebpageLoader {

    @Override
    public Document parseUrl(String url) {
        try {
            return Jsoup.connect(url).cookie("APID", UUID.randomUUID().toString()).get();
        } catch (IOException e) {
            log.error("Could not connect to url: {}.", url, e);
            return null;
        }
    }
}
