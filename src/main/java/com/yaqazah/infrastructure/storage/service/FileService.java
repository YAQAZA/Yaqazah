package com.yaqazah.infrastructure.storage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Base64;

@Service
public class FileService {

    @Autowired
    private S3Client s3Client;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name:snapshots}")
    private String bucketName;

    /**
     * Helper to process Base64 from the Controller
     */
    public String uploadBase64(String base64String, String fileName) {
        // Remove data:image/jpeg;base64, prefix if it exists
        String cleanBase64 = base64String.contains(",") ? base64String.split(",")[1] : base64String;
        byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64);

        uploadFile(bucketName, fileName, decodedBytes);
        return getTemporaryUrl(bucketName, fileName);
    }

    public void uploadFile(String bucket, String fileName, byte[] data) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType("image/jpeg")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));
    }

    public String getTemporaryUrl(String bucket, String key) {
        try (S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1)
                .build()) {

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        }
    }
}