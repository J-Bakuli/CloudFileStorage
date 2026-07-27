package com.jb.cloudstorage.cloud_storage.service;

import com.jb.cloudstorage.cloud_storage.dto.ResourceResponse;
import com.jb.cloudstorage.cloud_storage.exception.DirectoryAlreadyExistsException;
import com.jb.cloudstorage.cloud_storage.exception.FileAlreadyExistsException;
import com.jb.cloudstorage.cloud_storage.exception.InvalidCredentialsException;
import com.jb.cloudstorage.cloud_storage.exception.ResourceNotFoundException;
import com.jb.cloudstorage.cloud_storage.exception.StorageException;
import com.jb.cloudstorage.cloud_storage.model.ResourceType;
import com.jb.cloudstorage.cloud_storage.model.UserEntity;
import com.jb.cloudstorage.cloud_storage.repository.UserRepository;
import com.jb.cloudstorage.cloud_storage.util.FileUtils;
import io.minio.messages.Item;
import org.apache.coyote.BadRequestException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
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

        if (!fileStorageService.objectExists(userId, fullPath)) {
            throw new ResourceNotFoundException(String.format("Resource is not found, path=%s", fullPath));
        }
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

        List<Item> objects = fileStorageService.listObjects(userId, normalizedPath, false);
        return buildResponse(userId, objects);
    }

    public List<ResourceResponse> upload(String folderPath, List<MultipartFile> objects) throws Exception { //Todo remove throwing Exception later
        Long userId = getCurrentUserId();
        List<ResourceResponse> response = new ArrayList<>();

        for (MultipartFile object : objects) {
            String objectPath = FileUtils.joinPath(folderPath, object.getOriginalFilename());
            if (fileStorageService.objectExists(userId, objectPath)) {
                throw new FileAlreadyExistsException(String.format("File already exists, path=%s", objectPath));
            }

            fileStorageService.uploadFile(userId, folderPath, object);
            response = List.of(buildResponse(folderPath, object));
        }
        return response;
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
        ResourceType type = FileUtils.getResourceType(resourcePath);

        if (!resourceExists(userId, resourcePath, type)) {
            throw new ResourceNotFoundException(String.format("Resource is not found, path=%s", resourcePath));
        }

        fileStorageService.delete(userId, resourcePath);
    }

    public ResponseEntity<InputStreamResource> download(String resourcePath) throws Exception {
        Long userId = getCurrentUserId();
        ResourceType type = FileUtils.getResourceType(resourcePath);

        if (!resourceExists(userId, resourcePath, type)) {
            throw new ResourceNotFoundException(String.format("Resource is not found, path=%s", resourcePath));
        }

        String name = FileUtils.splitPath(resourcePath).name();
        if (type == ResourceType.DIRECTORY) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + ".zip\"")
                    .body(fileStorageService.downloadDirectoryAsZip(userId, resourcePath));
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .body(fileStorageService.download(userId, resourcePath));
    }

    public ResourceResponse move(String fromPath, String toPath) throws Exception {
        Long userId = getCurrentUserId();
        ResourceType fromType = FileUtils.getResourceType(fromPath);
        ResourceType toType = FileUtils.getResourceType(toPath);

        if (!resourceExists(userId, fromPath, fromType)) {
            throw new ResourceNotFoundException(String.format("Resource is not found, path=%s", fromPath));
        }

        if (resourceExists(userId, toPath, toType)) {
            if (toType == ResourceType.DIRECTORY) {
                throw new DirectoryAlreadyExistsException(String.format("Directory already exists, path=%s", toPath));
            }
            throw new FileAlreadyExistsException(String.format("File already exists, path=%s", toPath));
        }

        if (fromType == ResourceType.FILE) {
            fileStorageService.moveFile(userId, fromPath, toPath);
        } else {
            fileStorageService.moveDirectory(userId, fromPath, toPath);
        }

        FileUtils.PathParts pathParts = FileUtils.splitPath(toPath);
        Long size = pathParts.type() == ResourceType.FILE ? fileStorageService.getObjectSize(userId, toPath) : null;
        return new ResourceResponse(
                pathParts.parentPath(),
                pathParts.name(),
                size,
                pathParts.type()
        );
    }

    public List<ResourceResponse> search(String query) throws StorageException, BadRequestException {
        if (query.trim().isBlank()) {
            throw new BadRequestException("Query is empty");
        }
        Long userId = getCurrentUserId();
        List<Item> items = fileStorageService.search(userId, query);
        return buildResponse(userId, items);
    }

    private boolean resourceExists(Long userId, String resourcePath, ResourceType type) throws Exception {
        return type == ResourceType.FILE
                ? fileStorageService.objectExists(userId, resourcePath)
                : directoryExists(userId, resourcePath);
    }

    private boolean directoryExists(Long userId, String directoryPath) throws Exception {
        String path = FileUtils.normalizeParentPath(directoryPath);
        return fileStorageService.objectExists(userId, path)
                || !fileStorageService.listObjects(userId, path, false).isEmpty();
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

        return !fileStorageService.listObjects(userId, parentPath, false).isEmpty();
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
