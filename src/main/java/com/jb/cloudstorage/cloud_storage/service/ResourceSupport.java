package com.jb.cloudstorage.cloud_storage.service;

import com.jb.cloudstorage.cloud_storage.dto.ResourceResponse;
import com.jb.cloudstorage.cloud_storage.exception.InvalidCredentialsException;
import com.jb.cloudstorage.cloud_storage.exception.ResourceAlreadyExistsException;
import com.jb.cloudstorage.cloud_storage.model.ResourceType;
import com.jb.cloudstorage.cloud_storage.model.UserEntity;
import com.jb.cloudstorage.cloud_storage.repository.UserRepository;
import com.jb.cloudstorage.cloud_storage.util.FileUtils;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
class ResourceSupport {
    protected final UserRepository userRepository;
    protected final FileStorageService fileStorageService;

    void ensureNoCaseInsensitiveConflict(Long userId, String parentPath, String resourceName, ResourceType type) {
        ensureNoCaseInsensitiveConflict(userId, parentPath, resourceName, type, null);
    }

    void ensureNoCaseInsensitiveConflict(Long userId, String parentPath, String resourceName, ResourceType type,
                                         String excludePath) {
        String requestedName = resourceName.trim();
        String excludedRelativePath = excludePath == null ? null : FileUtils.normalizeParentPath(excludePath);
        List<Item> items = fileStorageService.listObjects(userId, parentPath, false);
        for (Item item : items) {
            String relativePath = FileUtils.getRelativePath(userId, item.objectName());
            if (isSameResource(relativePath, excludedRelativePath)) {
                continue;
            }

            FileUtils.PathParts candidate = FileUtils.splitPath(relativePath);
            if (candidate.type() != type || !candidate.name().equalsIgnoreCase(requestedName)) {
                continue;
            }

            if (type == ResourceType.DIRECTORY) {
                throw new ResourceAlreadyExistsException(String.format("Directory already exists, path=%s", resourceName));
            }
            throw new ResourceAlreadyExistsException(String.format("File already exists, path=%s", resourceName));
        }
    }

    private boolean isSameResource(String relativePath, String excludedRelativePath) {
        return excludedRelativePath != null
                && FileUtils.normalizeParentPath(relativePath).equalsIgnoreCase(excludedRelativePath);
    }

    boolean resourceExists(Long userId, String resourcePath, ResourceType type) {
        return type == ResourceType.FILE
                ? fileStorageService.objectExists(userId, resourcePath)
                : directoryExists(userId, resourcePath);
    }

    boolean directoryExists(Long userId, String directoryPath) {
        String path = FileUtils.normalizeParentPath(directoryPath);
        return fileStorageService.objectExists(userId, path)
                || !fileStorageService.listObjects(userId, path, false).isEmpty();
    }

    Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new InvalidCredentialsException(String.format("User with username=%s is not found", username));
        }
        return user.getId();
    }

    boolean parentExists(Long userId, String parentPath) {
        if (parentPath.isBlank()) {
            return true;
        }

        if (fileStorageService.objectExists(userId, parentPath)) {
            return true;
        }

        return !fileStorageService.listObjects(userId, parentPath, false).isEmpty();
    }

    List<ResourceResponse> buildResponse(Long userId, List<Item> objects) {
        return objects.stream().map(
                        item -> {
                            String objectName = item.objectName();
                            String relativePath = FileUtils.getRelativePath(userId, objectName);
                            FileUtils.PathParts parts = FileUtils.splitPath(relativePath);
                            ResourceType type = FileUtils.getResourceType(relativePath);
                            Long size = type == ResourceType.DIRECTORY ? null : item.size();
                            return new ResourceResponse(parts.parentPath(), parts.name(), size, type);
                        }
                )
                .sorted(Comparator.comparing(ResourceResponse::type))
                .toList();
    }
}
