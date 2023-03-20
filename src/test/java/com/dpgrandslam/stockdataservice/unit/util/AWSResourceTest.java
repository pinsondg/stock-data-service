package com.dpgrandslam.stockdataservice.unit.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.dpgrandslam.stockdataservice.domain.util.AWSResource;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

import java.io.*;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AWSResourceTest {

    @Mock
    private AmazonS3 amazonS3;

    @Mock
    private S3ObjectSummary s3ObjectSummary;

    private AWSResource subject;

    @Mock
    private S3Object s3Object;

    @Before
    public void setup() throws IOException {
        when(s3ObjectSummary.getKey()).thenReturn("folder/key.txt");
        when(s3ObjectSummary.getBucketName()).thenReturn("bucket");
        when(amazonS3.getObject(anyString(), anyString())).thenReturn(s3Object);
        when(s3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(new FileInputStream(createMockFile()), mock(HttpRequestBase.class)));
        subject = new AWSResource(amazonS3, s3ObjectSummary);
    }

    @Test
    public void testExists() throws IOException {
        boolean actual = subject.exists();

        assertTrue(actual);
    }

    @Test
    public void testGetInputStream() throws IOException {
        assertNotNull(subject.getInputStream());
    }

    @Test
    public void testGetUri() throws IOException {
        assertNotNull(subject.getURI());
    }

    @Test
    public void testGetURL() throws IOException {
        assertNotNull(subject.getURL());
    }

    @Test
    public void testGetSize() throws IOException {
        when(s3ObjectSummary.getSize()).thenReturn(10L);

        assertEquals(10L, subject.contentLength());
    }

    @Test
    public void testGetLastModified() throws IOException {
        Instant i = Instant.now();
        when(s3ObjectSummary.getLastModified()).thenReturn(Date.from(i));

        assertEquals(i.toEpochMilli(), subject.lastModified());
    }

    @Test
    public void testGetFileName() {
        assertNotNull(subject.getFilename());
    }

    @Test
    public void testCreateRelative() throws IOException {
        Resource r = subject.createRelative("some/path");
        assertNotNull(r);
        r.getFile().delete();
    }

    private File createMockFile() throws IOException {
        File f = File.createTempFile("temp", ".txt");
        BufferedWriter w = new BufferedWriter(new FileWriter(f));
        w.write("Hello");
        f.deleteOnExit();
        w.close();
        return f;
    }

}
