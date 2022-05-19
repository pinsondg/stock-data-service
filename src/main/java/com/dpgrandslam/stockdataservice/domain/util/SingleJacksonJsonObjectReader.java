package com.dpgrandslam.stockdataservice.domain.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.json.JsonObjectReader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SingleJacksonJsonObjectReader<T>  implements JsonObjectReader<T> {

    private final ObjectMapper objectMapper;
    private final Class<? extends T> itemType;
    private JsonParser jsonParser;
    private String jsonAsString;
    private boolean isRead;

    public SingleJacksonJsonObjectReader(Class<? extends T> itemType) {
        this(new ObjectMapper(), itemType);
    }

    public SingleJacksonJsonObjectReader(ObjectMapper objectMapper, Class<? extends T> itemType) {
        this.itemType = itemType;
        this.objectMapper = objectMapper;
        this.isRead = false;
    }

    @Override
    public T read() throws Exception {
        try {
            if (!this.isRead) {
                T obj = this.objectMapper.readValue(jsonAsString, this.itemType);
                this.isRead = true;
                return obj;
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new ParseException("Unable to read JSON object", e);
        }
    }

    @Override
    public void open(Resource resource) throws Exception {
        Assert.notNull(resource, "The resource must not be null");
        InputStream inputStream = resource.getInputStream();
        this.jsonAsString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        this.jsonParser = objectMapper.getFactory().createParser(jsonAsString);
        JsonToken firstToken = jsonParser.nextToken();
        Assert.state(firstToken == JsonToken.START_OBJECT || firstToken == JsonToken.START_ARRAY,
                "The Json input stream must start with an array of Json objects or a single json object");
    }

    @Override
    public void close() throws Exception {
        this.jsonParser.close();
    }
}
