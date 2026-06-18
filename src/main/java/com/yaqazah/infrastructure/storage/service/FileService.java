package com.yaqazah.infrastructure.storage.service;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class FileService {


    private final Cloudinary cloudinary;


    /**
     * Upload Base64 image
     */
    public String uploadBase64(String base64String, String fileName) {

        try {

            String cleanBase64 =
                    base64String.contains(",")
                            ? base64String.split(",")[1]
                            : base64String;


            byte[] bytes =
                    Base64.getDecoder().decode(cleanBase64);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader()
                    .upload(
                            bytes,
                            ObjectUtils.asMap(
                                    "public_id", fileName
                            )
                    );


            return result.get("secure_url").toString();


        } catch (IOException e) {
            throw new RuntimeException("Upload failed", e);
        }
    }


    /**
     * Upload MultipartFile
     */
    public String uploadFile(MultipartFile file) {

        try {

            Map result = cloudinary.uploader()
                    .upload(
                            file.getBytes(),
                            ObjectUtils.emptyMap()
                    );


            return result.get("secure_url").toString();


        } catch (IOException e) {
            throw new RuntimeException("Upload failed", e);
        }
    }


    /**
     * Delete image
     */
    public void deleteFile(String publicId) {

        try {

            cloudinary.uploader()
                    .destroy(
                            publicId,
                            ObjectUtils.emptyMap()
                    );

        } catch (IOException e) {
            throw new RuntimeException("Delete failed", e);
        }
    }
}


//package com.yaqazah.infrastructure.storage.service;
//
//import lombok.RequiredArgsConstructor;
//import org.jspecify.annotations.NullMarked;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
//import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.GetObjectRequest;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;
//import software.amazon.awssdk.services.s3.presigner.S3Presigner;
//import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
//
//import java.net.URI;
//import java.time.Duration;
//import java.util.Base64;
//
//@Service
//@RequiredArgsConstructor
//@NullMarked
//public class FileService {
//
//    private final S3Client s3Client;
//
//    @Value("${minio.endpoint}")
//    private String endpoint = "";
//
//    @Value("${minio.access-key}")
//    private String accessKey = "";
//
//    @Value("${minio.secret-key}")
//    private String secretKey = "";
//
//    @Value("${minio.bucket-name:snapshots}")
//    private String bucketName = "snapshots";
//
//    /**
//     * Helper to process Base64 from the Controller
//     */
//    public String uploadBase64(String base64String, String fileName) {
//        String cleanBase64 = base64String.contains(",") ? base64String.split(",")[1] : base64String;
//        byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64);
//
//        uploadFile(bucketName, fileName, decodedBytes);
//        return getTemporaryUrl(bucketName, fileName);
//    }
//
//    public void uploadFile(String bucket, String fileName, byte[] data) {
//        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//                .bucket(bucket)
//                .key(fileName)
//                .contentType("image/jpeg")
//                .build();
//
//        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));
//    }
//
//    public String getTemporaryUrl(String bucket, String key) {
//        try (S3Presigner presigner = S3Presigner.builder()
//                .endpointOverride(URI.create(endpoint))
//                .credentialsProvider(StaticCredentialsProvider.create(
//                        AwsBasicCredentials.create(accessKey, secretKey)))
//                .region(Region.US_EAST_1)
//                .build()) {
//
//            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
//                    .signatureDuration(Duration.ofMinutes(15))
//                    .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
//                    .build();
//
//            return presigner.presignGetObject(presignRequest).url().toString();
//        }
//    }
//}