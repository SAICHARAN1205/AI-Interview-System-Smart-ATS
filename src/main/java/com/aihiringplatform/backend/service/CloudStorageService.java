package com.aihiringplatform.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Service
public class CloudStorageService {

    private static final Logger logger = LoggerFactory.getLogger(CloudStorageService.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String publicUrlPrefix;

    public CloudStorageService(
            @Value("${cloud.aws.credentials.access-key:}") String accessKey,
            @Value("${cloud.aws.credentials.secret-key:}") String secretKey,
            @Value("${cloud.aws.region.static:us-east-1}") String region,
            @Value("${cloud.aws.s3.endpoint:}") String endpoint,
            @Value("${cloud.aws.s3.bucket-name:smartats-bucket}") String bucketName,
            @Value("${cloud.aws.s3.public-url-prefix:}") String publicUrlPrefix
    ) {
        this.bucketName = bucketName;
        this.publicUrlPrefix = publicUrlPrefix;

        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            
            var builder = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .region(Region.of(region));
                    
            if (!endpoint.isBlank()) {
                builder.endpointOverride(URI.create(endpoint));
            }
            
            this.s3Client = builder.build();
            logger.info("Cloud storage configured successfully with bucket: {}", bucketName);
        } else {
            this.s3Client = null;
            logger.warn("Cloud storage credentials missing! Falling back to null (mock) storage behavior.");
        }
    }

    public String uploadFile(MultipartFile file, String folderPath) throws IOException {
        if (s3Client == null) {
            logger.warn("Simulating upload since S3 is not configured: {}", file.getOriginalFilename());
            return "local-mock-path/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        }

        String extension = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String key = folderPath + "/" + UUID.randomUUID().toString() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        PutObjectResponse response = s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        
        logger.info("Uploaded file to cloud storage: {} (ETag: {})", key, response.eTag());

        if (!publicUrlPrefix.isBlank()) {
            return publicUrlPrefix + "/" + key;
        } else {
            return "s3://" + bucketName + "/" + key;
        }
    }

    public void deleteFile(String key) {
        if (s3Client == null) {
            logger.warn("Simulating delete since S3 is not configured: {}", key);
            return;
        }
        
        try {
            // If the key is a full URL, we need to extract the actual S3 key
            if (key.startsWith(publicUrlPrefix)) {
                key = key.substring(publicUrlPrefix.length() + 1);
            } else if (key.startsWith("s3://" + bucketName + "/")) {
                key = key.substring(("s3://" + bucketName + "/").length());
            }
            
            final String finalKey = key;
            s3Client.deleteObject(builder -> builder.bucket(bucketName).key(finalKey));
            logger.info("Deleted file from cloud storage: {}", key);
        } catch (Exception e) {
            logger.error("Failed to delete file from cloud storage: {}", key, e);
        }
    }
}
