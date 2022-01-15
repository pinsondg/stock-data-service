package com.dpgrandslam.stockdataservice.domain.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
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

public class AWSResource implements Resource {

    private AmazonS3 amazonS3;
    private S3ObjectSummary summary;
    private File downloadedFile;
    private Path path;

    public AWSResource(AmazonS3 amazonS3, S3ObjectSummary summary) {
        this.amazonS3 = amazonS3;
        this.summary = summary;
    }

    @Override
    public boolean exists() {
        try {
            return getFile().exists();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public URL getURL() throws IOException {
        if (downloadedFile == null) {
            return getFile().toURI().toURL();
        }
        return downloadedFile.toURI().toURL();
    }

    @Override
    public URI getURI() throws IOException {
        if (downloadedFile == null) {
            return getFile().toURI();
        }
        return downloadedFile.toURI();
    }

    @Override
    public File getFile() throws IOException {
        if (downloadedFile == null) {
            File file = File.createTempFile("tempAWSObject", summary.getKey().substring(summary.getKey().lastIndexOf('.')));
            S3Object o = amazonS3.getObject(summary.getBucketName(), summary.getKey());
            FileOutputStream fileInputStream = new FileOutputStream(file);
            IOUtils.copy(o.getObjectContent(), fileInputStream);
            fileInputStream.close();
            o.getObjectContent().close();
            downloadedFile = file;
            downloadedFile.deleteOnExit();
            path = downloadedFile.toPath();
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
        if (downloadedFile == null) {
            getFile();
        }
        String pathToUse = StringUtils.applyRelativePath(downloadedFile.getPath(), relativePath);
        return this.downloadedFile != null ? new FileSystemResource(pathToUse) : new FileSystemResource(this.path.getFileSystem(), pathToUse);
    }

    @Override
    public String getFilename() {
        if (downloadedFile == null) {
            try {
                return getFile().getName();
            } catch (IOException e) {
                return null;
            }
        }
        return downloadedFile.getName();
    }

    @Override
    public String getDescription() {
        return "fileLocation [" + path.toAbsolutePath() + "] s3Bucket [" + summary.getBucketName() + "]" + "s3Key [" + summary.getKey() + "]";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (downloadedFile == null) {
            getFile();
        }
        return Files.newInputStream(path);
    }
}
