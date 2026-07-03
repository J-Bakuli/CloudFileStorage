package com.jb.cloudstorage.cloud_storage.service;

import com.jb.cloudstorage.cloud_storage.config.MinioProperties;
import com.jb.cloudstorage.cloud_storage.util.FileUtils;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class FileStorageService {
    private final MinioProperties minioProperties;
    private final MinioClient minioClient;

    public FileStorageService(MinioProperties minioProperties, MinioClient minioClient) {
        this.minioProperties = minioProperties;
        this.minioClient = minioClient;
    }

    public void ensureBucketExists() throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(minioProperties.bucket())
                        .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(minioProperties.bucket())
                            .build()
            );
        }
    }

    public void uploadFile(Long userId, String relativePath, MultipartFile file) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        ensureBucketExists();

        String objectPath = FileUtils.joinPath(relativePath, file.getOriginalFilename());
        String objectName = fullObjectName(userId, objectPath);

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioProperties.bucket())
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
    }

    private String userRootPrefix(Long userId) {
        return "user-" + userId + "-files/";
    }

    private String fullObjectName(Long userId, String objectRelativePath) {
        return userRootPrefix(userId) + objectRelativePath;
    }
}
