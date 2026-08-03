package com.jb.cloudstorage.cloud_storage.config;

import com.jb.cloudstorage.cloud_storage.exception.StorageException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {
    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Bean
    public ApplicationRunner init(MinioClient minioClient, MinioProperties minioProperties) {
        return args -> ensureBucketExists(minioClient, minioProperties);
    }

    private void ensureBucketExists(MinioClient minioClient, MinioProperties minioProperties) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioProperties.bucket())
                            .build()
            );

            if (!exists) {
                log.info("Creating MinIO bucket: {}", minioProperties.bucket());
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioProperties.bucket())
                                .build()
                );
            }
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageException("Storage error", e);
        }
    }
}
