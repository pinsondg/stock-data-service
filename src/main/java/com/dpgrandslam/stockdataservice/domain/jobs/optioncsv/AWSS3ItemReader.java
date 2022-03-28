package com.dpgrandslam.stockdataservice.domain.jobs.optioncsv;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.dpgrandslam.stockdataservice.domain.model.OptionCSVFile;
import com.dpgrandslam.stockdataservice.domain.util.AWSResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class AWSS3ItemReader extends MultiResourceItemReader<OptionCSVFile> implements InitializingBean {

    private List<S3ObjectSummary> s3Objects;
    private final AmazonS3 amazonS3;
    private String bucket;
    private String keyPrefix;

    public AWSS3ItemReader(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
        this.s3Objects = new ArrayList<>();
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
        setObjectSummaries();
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
        setObjectSummaries();
    }

    @Override
    public OptionCSVFile read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        OptionCSVFile optionCSVFile =  super.read();
        Resource resource = super.getCurrentResource();
        if (resource != null && resource.getFile() != null && resource.getFile().exists()) {
            resource.getFile().delete();
        }
        return optionCSVFile;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        super.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        super.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        super.close();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.bucket, "A S3 bucket is required.");
        Assert.notNull(this.keyPrefix, "A key prefix is required.");
    }

    private void setObjectSummaries() {
        if (StringUtils.isNoneBlank(this.bucket) && StringUtils.isNoneBlank(this.keyPrefix)) {
            if (s3Objects == null || s3Objects.isEmpty()) {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucket).withPrefix(keyPrefix);
                s3Objects = amazonS3.listObjects(listObjectsRequest).getObjectSummaries().stream().filter(x -> !x.getKey().endsWith("/")).collect(Collectors.toList());
            }
            super.setResources(s3Objects.stream().map(x -> {
                try {
                    return new AWSResource(amazonS3, x);
                } catch (IOException e) {
                    log.error("Error creating S3 resource from bucket: {} with key: {}.", x.getBucketName(), x.getKey(), e);
                    return null;
                }
            }).filter(Objects::nonNull).toArray(Resource[]::new));
        }
    }
}
