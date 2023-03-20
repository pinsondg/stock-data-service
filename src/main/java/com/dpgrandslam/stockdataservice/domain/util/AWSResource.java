package com.dpgrandslam.stockdataservice.domain.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class AWSResource implements Resource {

    private AmazonS3 amazonS3;
    private S3ObjectSummary summary;
    private File downloadedFile;
    private Path path;
    private boolean isDownloaded;


    public AWSResource(AmazonS3 amazonS3, S3ObjectSummary summary) throws IOException {
        this.amazonS3 = amazonS3;
        this.summary = summary;
        isDownloaded = false;
        createFile();
    }

    private void createFile() throws IOException {
        int separatorIndex = summary.getKey().lastIndexOf("/");
        String fileName;
        if (separatorIndex != -1) {
            fileName = summary.getKey().substring(separatorIndex + 1);
        } else {
            fileName = summary.getKey();
        }
        String name;
        if (fileName.lastIndexOf('.') > 0) {
            name = fileName.substring(0, fileName.lastIndexOf('.'));
        } else {
            name = fileName;
        }
        downloadedFile = File.createTempFile("tempAWSObject-" + name, fileName.substring(fileName.lastIndexOf('.')));
        downloadedFile.deleteOnExit();
        path = downloadedFile.toPath();
    }

    @Override
    public boolean exists() {
        return downloadedFile.exists();
    }

    @Override
    public URL getURL() throws IOException {
        return downloadedFile.toURI().toURL();
    }

    @Override
    public URI getURI() throws IOException {
        return downloadedFile.toURI();
    }

    @Override
    public File getFile() throws IOException {
        if (!isDownloaded) {
            log.info("Downloading file from S3 from bucket: {} with key: {}", summary.getBucketName(), summary.getKey());
            S3Object o = amazonS3.getObject(summary.getBucketName(), summary.getKey());
            FileOutputStream fileInputStream = new FileOutputStream(downloadedFile);
            IOUtils.copy(o.getObjectContent(), fileInputStream);
            fileInputStream.close();
            o.getObjectContent().close();
            log.info("File downloaded successfully!");
            isDownloaded = true;
        }
        return downloadedFile;
    }

    @Override
    public long contentLength() throws IOException {
        return summary.getSize();
    }

    @Override
    public long lastModified() throws IOException {
        return summary.getLastModified().getTime();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        String pathToUse = StringUtils.applyRelativePath(downloadedFile.getPath(), relativePath);
        return this.downloadedFile != null ? new FileSystemResource(pathToUse) : new FileSystemResource(this.path.getFileSystem(), pathToUse);
    }

    @Override
    public String getFilename() {
        return downloadedFile.getName();
    }

    @Override
    public String getDescription() {
        return "fileLocation [" + path.toAbsolutePath() + "] s3Bucket [" + summary.getBucketName() + "]" + "s3Key [" + summary.getKey() + "]";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (!isDownloaded) {
            getFile();
        }
        return Files.newInputStream(path);
    }
}
