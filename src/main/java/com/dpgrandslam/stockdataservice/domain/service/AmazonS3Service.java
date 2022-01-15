package com.dpgrandslam.stockdataservice.domain.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class AmazonS3Service {

    private final AmazonS3 s3;

    public List<File> downloadFilesWithPrefix(String bucketName, String keyPrefix) {
        List<File> downloadedFiles = new ArrayList<>();

        ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
        listObjectsV2Request.setBucketName(bucketName);
        listObjectsV2Request.setPrefix(keyPrefix);

        ListObjectsV2Result objects = s3.listObjectsV2(listObjectsV2Request);
        objects.getObjectSummaries().forEach(summary -> {
            S3Object o = s3.getObject(bucketName, summary.getKey());
            File tempFile = null;
            try {
                tempFile = File.createTempFile("optionCSVData", ".csv");
                tempFile.deleteOnExit();
                S3ObjectInputStream content = o.getObjectContent();
                FileOutputStream outputStream = new FileOutputStream(tempFile);
                IOUtils.copy(content, outputStream);
                content.close();
                outputStream.close();
                downloadedFiles.add(tempFile);
            } catch (IOException e) {
                log.error("Error downloading file from S3. Bucket: {}, Key: {}", bucketName, summary.getKey(), e);
            }
        });
        return downloadedFiles;
    }
}
