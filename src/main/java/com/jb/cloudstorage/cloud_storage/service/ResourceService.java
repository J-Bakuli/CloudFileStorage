package com.jb.cloudstorage.cloud_storage.service;

import com.jb.cloudstorage.cloud_storage.dto.ResourceResponse;
import com.jb.cloudstorage.cloud_storage.exception.DirectoryAlreadyExistsException;
import com.jb.cloudstorage.cloud_storage.exception.FileAlreadyExistsException;
import com.jb.cloudstorage.cloud_storage.exception.InvalidCredentialsException;
import com.jb.cloudstorage.cloud_storage.exception.ResourceNotFoundException;
import com.jb.cloudstorage.cloud_storage.model.ResourceType;
import com.jb.cloudstorage.cloud_storage.model.UserEntity;
import com.jb.cloudstorage.cloud_storage.repository.UserRepository;
import com.jb.cloudstorage.cloud_storage.util.FileUtils;
import io.minio.messages.Item;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ResourceService {
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public ResourceService(UserRepository userRepository, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    public ResourceResponse get(String fullPath) throws Exception {
        Long userId = getCurrentUserId();

        FileUtils.PathParts pathParts = FileUtils.splitPath(fullPath);

        Long size = pathParts.type() == ResourceType.FILE
                ? fileStorageService.getObjectSize(userId, fullPath)
                : null;
        return new ResourceResponse(
                pathParts.parentPath(),
                pathParts.name(),
                size,
                pathParts.type());
    }

    public List<ResourceResponse> getDirectory(String fullPath) throws Exception {
        Long userId = getCurrentUserId();

        String normalizedPath = FileUtils.normalizeParentPath(fullPath);

        if (!parentExists(userId, normalizedPath)) {
            throw new ResourceNotFoundException(String.format("Directory is not found, path=%s", normalizedPath));
        }

        List<Item> objects = fileStorageService.listObjects(userId, normalizedPath);
        return buildResponse(userId, objects);
    }

    public List<ResourceResponse> upload(String folderPath, MultipartFile file) throws Exception { //Todo remove throwing Exception later
        Long userId = getCurrentUserId();

        String objectPath = FileUtils.joinPath(folderPath, file.getOriginalFilename());
        if (fileStorageService.objectExists(userId, objectPath)) {
            throw new FileAlreadyExistsException(String.format("File already exists, path=%s", objectPath));
        }

        fileStorageService.uploadFile(userId, folderPath, file);
        return List.of(buildResponse(folderPath, file));
    }

    public ResourceResponse createDirectory(String directoryPath) throws Exception {
        Long userId = getCurrentUserId();
        String normalizedPath = FileUtils.normalizeParentPath(directoryPath);

        FileUtils.PathParts parts = FileUtils.splitPath(normalizedPath);
        String parentPath = parts.parentPath();

        if (fileStorageService.objectExists(userId, normalizedPath)) {
            throw new DirectoryAlreadyExistsException(String.format("Directory already exists, path=%s", directoryPath));
        }

        if (!parentExists(userId, parentPath)) {
            throw new ResourceNotFoundException(String.format("Parent directory is not found, path=%s", parentPath));
        }

        fileStorageService.createDirectory(userId, normalizedPath);

        return new ResourceResponse(
                parts.parentPath(),
                parts.name(),
                null,
                ResourceType.DIRECTORY);
    }

    public void delete(String resourcePath) throws Exception {
        Long userId = getCurrentUserId();

        if (!fileStorageService.objectExists(userId, resourcePath)) {
            throw new ResourceNotFoundException(String.format("Resource is not found, path=%s", resourcePath));
        }

        fileStorageService.delete(userId, resourcePath);
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new InvalidCredentialsException(String.format("User with username=%s is not found", username));
        }
        return user.getId();
    }

    private boolean parentExists(Long userId, String parentPath) throws Exception {
        if (parentPath.isBlank()) {
            return true;
        }

        if (fileStorageService.objectExists(userId, parentPath)) {
            return true;
        }

        return !fileStorageService.listObjects(userId, parentPath).isEmpty();
    }

    private ResourceResponse buildResponse(String folderPath, MultipartFile file) {
        return new ResourceResponse(
                FileUtils.normalizeParentPath(folderPath),
                file.getOriginalFilename(),
                file.getSize(),
                ResourceType.FILE
        );
    }

    private List<ResourceResponse> buildResponse(Long userId, List<Item> objects) {
        return objects.stream().map(
                item -> {
                    String objectName = item.objectName();
                    String relativePath = objectName.substring(("user-" + userId + "-files/").length());
                    FileUtils.PathParts parts = FileUtils.splitPath(relativePath);
                    Long size = item.isDir() ? null : item.size();
                    ResourceType type = item.isDir() ? ResourceType.DIRECTORY : ResourceType.FILE;
                    return new ResourceResponse(parts.parentPath(), parts.name(), size, type);
                }
        ).toList();
    }
}
