package com.jb.cloudstorage.cloud_storage.service;

import com.jb.cloudstorage.cloud_storage.dto.ResourceResponse;
import com.jb.cloudstorage.cloud_storage.exception.DirectoryAlreadyExistsException;
import com.jb.cloudstorage.cloud_storage.exception.FileAlreadyExistsException;
import com.jb.cloudstorage.cloud_storage.exception.InvalidCredentialsException;
import com.jb.cloudstorage.cloud_storage.exception.InvalidRequestException;
import com.jb.cloudstorage.cloud_storage.exception.ResourceNotFoundException;
import com.jb.cloudstorage.cloud_storage.model.ResourceType;
import com.jb.cloudstorage.cloud_storage.model.UserEntity;
import com.jb.cloudstorage.cloud_storage.repository.UserRepository;
import com.jb.cloudstorage.cloud_storage.util.FileUtils;
import com.jb.cloudstorage.cloud_storage.util.ResourceNameValidator;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ResourceService {
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public ResourceResponse get(String fullPath) {
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

    public List<ResourceResponse> getDirectory(String fullPath) {
        Long userId = getCurrentUserId();

        String normalizedPath = FileUtils.normalizeParentPath(fullPath);

        if (!parentExists(userId, normalizedPath)) {
            throw new ResourceNotFoundException(String.format("Directory is not found, path=%s", normalizedPath));
        }

        List<Item> objects = fileStorageService.listObjects(userId, normalizedPath, false);
        return buildResponse(userId, objects);
    }

    public List<ResourceResponse> upload(String folderPath, List<MultipartFile> objects) {
        Long userId = getCurrentUserId();
        List<ResourceResponse> response = new ArrayList<>();
        String normalizedPath = FileUtils.normalizeParentPath(folderPath);
        Set<String> uploadedNames = new HashSet<>();

        for (MultipartFile object : objects) {
            if (object == null || object.isEmpty()) {
                throw new InvalidRequestException("File is null or empty");
            }
            String filename = object.getOriginalFilename();
            if (!StringUtils.hasText(filename)) {
                throw new InvalidRequestException("File name is missing");
            }
            if (!ResourceNameValidator.isSafeName(filename)) {
                throw new InvalidRequestException("Invalid filename");
            }
            String objectPath = FileUtils.joinPath(folderPath, filename);
            if (fileStorageService.objectExists(userId, objectPath)) {
                throw new FileAlreadyExistsException(String.format("File already exists, path=%s", objectPath));
            }
            ensureNoCaseInsensitiveConflict(userId, normalizedPath, filename, ResourceType.FILE);
            String key = filename.toLowerCase(Locale.ROOT);
            if (uploadedNames.contains(key)) {
                throw new FileAlreadyExistsException(String.format("File already exists, path=%s", objectPath));
            }
            fileStorageService.uploadFile(userId, folderPath, object);
            uploadedNames.add(key);
            response = List.of(buildResponse(folderPath, object));
        }
        return response;
    }

    public ResourceResponse createDirectory(String directoryPath) {
        Long userId = getCurrentUserId();
        String normalizedPath = FileUtils.normalizeParentPath(directoryPath);

        FileUtils.PathParts parts = FileUtils.splitPath(normalizedPath);
        String parentPath = parts.parentPath();
        String requestedName = parts.name();

        if (fileStorageService.objectExists(userId, normalizedPath)) {
            throw new DirectoryAlreadyExistsException(String.format("Directory already exists, path=%s", directoryPath));
        }

        if (!parentExists(userId, parentPath)) {
            throw new ResourceNotFoundException(String.format("Parent directory is not found, path=%s", parentPath));
        }

        ensureNoCaseInsensitiveConflict(userId, parentPath, requestedName, ResourceType.DIRECTORY);

        fileStorageService.createDirectory(userId, normalizedPath);

        return new ResourceResponse(
                parts.parentPath(),
                parts.name(),
                null,
                ResourceType.DIRECTORY);
    }

    public void delete(String resourcePath) {
        Long userId = getCurrentUserId();
        ResourceType type = FileUtils.getResourceType(resourcePath);

        if (!resourceExists(userId, resourcePath, type)) {
            throw new ResourceNotFoundException(String.format("Resource is not found, path=%s", resourcePath));
        }

        fileStorageService.delete(userId, resourcePath);
    }

    public ResponseEntity<InputStreamResource> download(String resourcePath) {
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

    public ResourceResponse move(String fromPath, String toPath) {
        Long userId = getCurrentUserId();
        ResourceType fromType = FileUtils.getResourceType(fromPath);
        ResourceType toType = FileUtils.getResourceType(toPath);

        String normalizedPath = FileUtils.normalizeParentPath(toPath);
        FileUtils.PathParts parts = FileUtils.splitPath(normalizedPath);
        String parentPath = parts.parentPath();
        String requestedName = parts.name();

        if (!resourceExists(userId, fromPath, fromType)) {
            throw new ResourceNotFoundException(String.format("Resource is not found, path=%s", fromPath));
        }

        if (resourceExists(userId, toPath, toType)) {
            if (toType == ResourceType.DIRECTORY) {
                throw new DirectoryAlreadyExistsException(String.format("Directory already exists, path=%s", toPath));
            }
            throw new FileAlreadyExistsException(String.format("File already exists, path=%s", toPath));
        }

        ensureNoCaseInsensitiveConflict(userId, parentPath, requestedName, toType);

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

    public List<ResourceResponse> search(String query) {
        if (query.trim().isBlank()) {
            throw new InvalidRequestException("Query is empty");
        }
        Long userId = getCurrentUserId();
        List<Item> items = fileStorageService.search(userId, query);
        return buildResponse(userId, items);
    }

    private void ensureNoCaseInsensitiveConflict(Long userId, String parentPath, String resourceName, ResourceType type) {
        List<Item> items = fileStorageService.listObjects(userId, parentPath, false);
        for (Item item : items) {
            String objectName = item.objectName();
            String relativePath = FileUtils.getRelativePath(userId, objectName);
            FileUtils.PathParts pathParts = FileUtils.splitPath(relativePath);

            if (pathParts.type() == type && pathParts.name().equalsIgnoreCase(resourceName.trim())) {
                if (type == ResourceType.DIRECTORY) {
                    throw new DirectoryAlreadyExistsException(String.format("Directory already exists, path=%s", resourceName));
                }
                throw new FileAlreadyExistsException(String.format("File already exists, path=%s", resourceName));
            }
        }
    }

    private boolean resourceExists(Long userId, String resourcePath, ResourceType type) {
        return type == ResourceType.FILE
                ? fileStorageService.objectExists(userId, resourcePath)
                : directoryExists(userId, resourcePath);
    }

    private boolean directoryExists(Long userId, String directoryPath) {
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

    private boolean parentExists(Long userId, String parentPath) {
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
