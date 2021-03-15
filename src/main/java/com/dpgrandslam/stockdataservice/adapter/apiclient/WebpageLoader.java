package com.dpgrandslam.stockdataservice.adapter.apiclient;

import org.jsoup.nodes.Document;

import java.io.IOException;

@FunctionalInterface
public interface WebpageLoader {
    Document parseUrl(String url);
}
