package com.jb.cloudstorage.cloud_storage.service;

import com.jb.cloudstorage.cloud_storage.config.MinioProperties;
import com.jb.cloudstorage.cloud_storage.exception.StorageException;
import com.jb.cloudstorage.cloud_storage.model.ResourceType;
import com.jb.cloudstorage.cloud_storage.util.FileUtils;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileStorageService {
    private final MinioProperties minioProperties;
    private final MinioClient minioClient;

    public void uploadFile(Long userId, String relativePath, MultipartFile file) {
        log.debug("Uploading file for userId={}, relativePath={}, filename={}, size={}",
                userId, relativePath, file.getOriginalFilename(), file.getSize());
        ensureBucketExists();
        try {
            String objectPath = FileUtils.joinPath(relativePath, file.getOriginalFilename());
            String objectName = fullObjectName(userId, objectPath);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("Uploaded file for userId={}, objectName={}", userId, objectName);
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageException("Storage error", e);
        }
    }

    public void createDirectory(Long userId, String folderPath) {
        log.debug("Creating directory for userId={}, folderPath={}", userId, folderPath);
        ensureBucketExists();
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(fullObjectName(userId, folderPath))
                    .stream(InputStream.nullInputStream(), 0, 0)
                    .contentType("application/octet-stream")
                    .build());
            log.info("Created directory for userId={}, folderPath={}", userId, folderPath);
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageException("Storage error", e);
        }
    }

    public boolean objectExists(Long userId, String directoryPath) {
        log.debug("Checking object existence for userId={}, path={}", userId, directoryPath);
        ensureBucketExists();
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(fullObjectName(userId, directoryPath))
                            .build()
            );
            log.debug("Object exists for userId={}, path={}", userId, directoryPath);
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code()) || "NoSuchBucket".equals(e.errorResponse().code())) {
                log.debug("Object not found for userId={}, path={}", userId, directoryPath);
                return false;
            }
            log.warn("MinIO error while checking object for userId={}, path={}, code={}",
                    userId, directoryPath, e.errorResponse().code());
            throw new StorageException("Storage error", e);
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageException("Storage error", e);
        }
    }

    public Long getObjectSize(Long userId, String fullPath) {
        log.debug("Getting object size for userId={}, path={}", userId, fullPath);
        ensureBucketExists();
        String objectName = fullObjectName(userId, fullPath);
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectName)
                            .build()
            ).size();
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageException("Storage error", e);
        }
    }

    public void delete(Long userId, String resourcePath) {
        ensureBucketExists();
        ResourceType type = FileUtils.getResourceType(resourcePath);
        log.debug("Deleting resource for userId={}, path={}, type={}", userId, resourcePath, type);
        try {
            if (type == ResourceType.FILE) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(minioProperties.bucket())
                        .object(fullObjectName(userId, resourcePath))
                        .build());
                log.info("Deleted file for userId={}, path={}", userId, resourcePath);
            } else {
                deleteRecursively(userId, resourcePath);
            }
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageException("Storage error", e);
        }
    }

    public InputStreamResource download(Long userId, String resourcePath) {
        ensureBucketExists();
        String objectName = fullObjectName(userId, resourcePath);
        log.debug("Downloading file for userId={}, objectName={}", userId, objectName);
        try {
            return new InputStreamResource(minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(objectName)
                    .build()));
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageException("Storage error", e);
        }
    }

    public InputStreamResource downloadDirectoryAsZip(Long userId, String resourcePath) {
        ensureBucketExists();
        log.debug("Downloading directory as zip for userId={}, path={}", userId, resourcePath);

        List<Item> results = listObjects(userId, resourcePath, true);
        String normalizedPath = FileUtils.normalizeParentPath(resourcePath);
        String prefix = buildUserObjectPrefix(userId, normalizedPath);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (Item item : results) {
                if (item.isDir()) {
                    continue;
                }

                String objectName = item.objectName();
                String archiveEntryName = objectName.substring(prefix.length());
                if (archiveEntryName.isEmpty()) {
                    continue;
                }

                log.debug("Adding to zip: {} -> {}", objectName, archiveEntryName);
                try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(minioProperties.bucket())
                        .object(objectName)
                        .build())) {
                    zipOutputStream.putNextEntry(new ZipEntry(archiveEntryName));
                    inputStream.transferTo(zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageException("Storage error", e);
        }

        log.info("Built zip for userId={}, path={}, size={}", userId, resourcePath, outputStream.size());
        return new InputStreamResource(new ByteArrayInputStream(outputStream.toByteArray()));
    }

    public void moveFile(Long userId, String fromPath, String toPath) {
        log.debug("Move file for userId={}, fromPath={}, toPath={}", userId, fromPath, toPath);
        ensureBucketExists();
        copyObject(userId, fromPath, toPath);
        delete(userId, fromPath);
        log.info("Moved file for userId={}, fromPath={}, toPath={}", userId, fromPath, toPath);
    }

    public void moveDirectory(Long userId, String fromPath, String toPath) {
        log.debug("Move directory for userId={}, fromPath={}, toPath={}", userId, fromPath, toPath);
        ensureBucketExists();
        copyObjectRecursively(userId, fromPath, toPath);
        deleteRecursively(userId, fromPath);
        log.info("Moved directory for userId={}, fromPath={}, toPath={}", userId, fromPath, toPath);
    }

    public List<Item> search(Long userId, String query) {
        log.debug("Search files for userId={}, query={}", userId, query);
        List<Item> items = listObjects(userId, "", true);
        log.info("Search is completed for userId={}, query={}", userId, query);
        return filter(userId, query, items);
    }

    public List<Item> listObjects(Long userId, String directoryPath, boolean isRecursive) {
        log.debug("Listing objects for userId={}, directoryPath={}", userId, directoryPath);
        try {
            ensureBucketExists();

            String prefix = buildUserObjectPrefix(userId, directoryPath);

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.bucket())
                            .prefix(prefix)
                            .recursive(isRecursive)
                            .build()
            );

            List<Item> items = new ArrayList<>();
            for (Result<Item> result : results) {
                items.add(result.get());
            }
            items.removeIf(item -> item.objectName().equals(prefix));
            log.debug("Listed {} objects for userId={}, directoryPath={}", items.size(), userId, directoryPath);
            return items;
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageException("Storage error", e);
        }
    }

    private void ensureBucketExists() {
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

    private List<Item> filter(Long userId, String query, List<Item> items) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>(items);
        }
        String lowerQuery = query.trim().toLowerCase();
        return items.stream().filter(
                        item -> {
                            String objectName = item.objectName();
                            String relativePath = FileUtils.getRelativePath(userId, objectName);
                            return relativePath.toLowerCase().contains(lowerQuery);
                        }
                )
                .toList();
    }

    private void copyObject(Long userId, String fromPath, String toPath) {
        try {
            String fromObjectName = fullObjectName(userId, fromPath);
            String toObjectName = fullObjectName(userId, toPath);

            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(toObjectName)
                    .source(CopySource.builder()
                            .bucket(minioProperties.bucket())
                            .object(fromObjectName)
                            .build())
                    .build());
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageException("Storage error", e);
        }
    }

    private void copyObjectRecursively(Long userId, String fromPath, String toPath) {
        String fromPrefix = buildUserObjectPrefix(userId, fromPath);
        String toPrefix = buildUserObjectPrefix(userId, toPath);

        log.debug("Copying objects recursively for userId={}, path={}, fromPrefix={}, toPrefix={}", userId, fromPath, fromPrefix, toPrefix);
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.bucket())
                            .prefix(fromPrefix)
                            .recursive(true)
                            .build()
            );

            int copiedCount = 0;
            for (Result<Item> result : results) {
                String fromObjectName = result.get().objectName();
                String toObjectName = toPrefix + fromObjectName.substring(fromPrefix.length());
                minioClient.copyObject(CopyObjectArgs.builder()
                        .bucket(minioProperties.bucket())
                        .object(toObjectName)
                        .source(CopySource.builder()
                                .bucket(minioProperties.bucket())
                                .object(fromObjectName)
                                .build())
                        .build());
                copiedCount++;
                log.debug("Copied object: {}", toObjectName);
            }
            log.info("Copied files in directory for userId={}, fromPath={}, toPath={},objectsCopied={}",
                    userId, fromPath, toPath, copiedCount);
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageException("Storage error", e);
        }
    }

    private void deleteRecursively(Long userId, String resourcePath) {
        String normalizedPath = FileUtils.normalizeParentPath(resourcePath);
        String prefix = buildUserObjectPrefix(userId, normalizedPath);
        log.debug("Deleting directory recursively for userId={}, path={}, prefix={}",
                userId, resourcePath, prefix);
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.bucket())
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            int deletedCount = 0;
            for (Result<Item> result : results) {
                String objectName = result.get().objectName();
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(minioProperties.bucket())
                        .object(objectName)
                        .build()
                );
                deletedCount++;
                log.debug("Removed object: {}", objectName);
            }
            log.info("Deleted directory for userId={}, path={}, objectsRemoved={}", userId, resourcePath, deletedCount);
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageException("Storage error", e);
        }
    }

    private String fullObjectName(Long userId, String objectRelativePath) {
        return FileUtils.userRootPrefix(userId) + objectRelativePath;
    }

    private String buildUserObjectPrefix(Long userId, String directoryPath) {
        String normalizedPath = FileUtils.normalizeParentPath(directoryPath);
        return fullObjectName(userId, normalizedPath);
    }
}
