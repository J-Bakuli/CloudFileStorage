package com.jb.cloudstorage.cloud_storage.service;

import com.jb.cloudstorage.cloud_storage.dto.ResourceResponse;
import com.jb.cloudstorage.cloud_storage.exception.InvalidRequestException;
import com.jb.cloudstorage.cloud_storage.exception.ResourceAlreadyExistsException;
import com.jb.cloudstorage.cloud_storage.exception.ResourceNotFoundException;
import com.jb.cloudstorage.cloud_storage.model.ResourceType;
import com.jb.cloudstorage.cloud_storage.repository.UserRepository;
import com.jb.cloudstorage.cloud_storage.util.FileUtils;
import com.jb.cloudstorage.cloud_storage.util.ResourceNameValidator;
import io.minio.messages.Item;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ResourceService extends ResourceSupport {
    public ResourceService(UserRepository userRepository, FileStorageService fileStorageService) {
        super(userRepository, fileStorageService);
    }

    public ResourceResponse get(Long userId, String fullPath) {
        FileUtils.PathParts pathParts = FileUtils.splitPath(fullPath);

        if (!resourceExists(userId, fullPath, pathParts.type())) {
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

    public List<ResourceResponse> getDirectory(Long userId, String fullPath) {
        if (!fullPath.isBlank() && !fullPath.endsWith("/")) {
            throw new InvalidRequestException(String.format("Directory path must end with /, path=%s", fullPath));
        }

        String normalizedPath = FileUtils.normalizeParentPath(fullPath);

        if (!parentExists(userId, normalizedPath)) {
            throw new ResourceNotFoundException(String.format("Directory is not found, path=%s", normalizedPath));
        }

        List<Item> objects = fileStorageService.listObjects(userId, normalizedPath, false);
        return buildResponse(userId, objects);
    }

    public List<ResourceResponse> upload(Long userId, String folderPath, List<MultipartFile> objects) {
        List<ResourceResponse> response = new ArrayList<>();
        Set<String> uploadedNames = new HashSet<>();

        for (MultipartFile object : objects) {
            if (object == null || object.isEmpty()) {
                throw new InvalidRequestException("File is null or empty");
            }
            String filename = object.getOriginalFilename();
            if (!StringUtils.hasText(filename)) {
                throw new InvalidRequestException("File name is missing");
            }
            if (!ResourceNameValidator.isSafeUploadFileName(filename)) {
                throw new InvalidRequestException("Invalid filename");
            }
            String objectPath = FileUtils.joinPath(folderPath, filename);
            FileUtils.PathParts objectParts = FileUtils.splitPath(objectPath);
            ensureNoCaseInsensitiveConflict(userId, objectParts.parentPath(), objectParts.name(), ResourceType.FILE);
            String key = objectPath.toLowerCase(Locale.ROOT);
            if (uploadedNames.contains(key)) {
                throw new ResourceAlreadyExistsException(String.format("File already exists, path=%s", objectPath));
            }
            fileStorageService.uploadFile(userId, folderPath, object);
            uploadedNames.add(key);
            response.add(new ResourceResponse(
                    objectParts.parentPath(),
                    objectParts.name(),
                    object.getSize(),
                    ResourceType.FILE
            ));
        }
        return response;
    }

    public ResourceResponse createDirectory(Long userId, String directoryPath) {
        if (!directoryPath.endsWith("/")) {
            throw new InvalidRequestException(String.format("Directory path must end with /, path=%s", directoryPath));
        }

        String normalizedPath = FileUtils.normalizeParentPath(directoryPath);

        FileUtils.PathParts parts = FileUtils.splitPath(normalizedPath);
        String parentPath = parts.parentPath();
        String requestedName = parts.name();

        if (fileStorageService.objectExists(userId, normalizedPath)) {
            throw new ResourceAlreadyExistsException(String.format("Directory already exists, path=%s", directoryPath));
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

    public void delete(Long userId, String resourcePath) {
        ResourceType type = FileUtils.getResourceType(resourcePath);

        if (!resourceExists(userId, resourcePath, type)) {
            throw new ResourceNotFoundException(String.format("Resource is not found, path=%s", resourcePath));
        }

        fileStorageService.delete(userId, resourcePath);
    }

    public ResponseEntity<InputStreamResource> download(Long userId, String resourcePath) {
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

    public ResourceResponse move(Long userId, String fromPath, String toPath) {
        ResourceType fromType = FileUtils.getResourceType(fromPath);
        ResourceType toType = FileUtils.getResourceType(toPath);

        if (fromType != toType) {
            throw new InvalidRequestException(String.format("Invalid operation request, cannot move, fromType=%s, toType=%s",
                    fromType, toType));
        }

        if (!resourceExists(userId, fromPath, fromType)) {
            throw new ResourceNotFoundException(String.format("Resource is not found, path=%s", fromPath));
        }

        String normalizedToPath = FileUtils.normalizeParentPath(toPath);
        String normalizedFromPath = FileUtils.normalizeParentPath(fromPath);
        FileUtils.PathParts parts = FileUtils.splitPath(normalizedToPath);
        String parentPath = parts.parentPath();
        String requestedName = parts.name();

        if (toType == ResourceType.FILE && toPath.equals(fromPath)) {
            throw new InvalidRequestException(String.format("Invalid operation request, cannot move, fromPath=%s, toPath=%s",
                    fromPath, toPath));
        } else if (toType == ResourceType.DIRECTORY && normalizedToPath.startsWith(normalizedFromPath)) {
            throw new InvalidRequestException(String.format("Invalid operation request, cannot move, fromPath=%s, toPath=%s",
                    fromPath, toPath));
        }

        if (resourceExists(userId, toPath, toType)) {
            if (toType == ResourceType.DIRECTORY) {
                throw new ResourceAlreadyExistsException(String.format("Directory already exists, path=%s", toPath));
            }
            throw new ResourceAlreadyExistsException(String.format("File already exists, path=%s", toPath));
        }

        if (!parentExists(userId, parentPath)) {
            throw new ResourceNotFoundException(String.format("Parent directory is not found, path=%s", parentPath));
        }

        ensureNoCaseInsensitiveConflict(userId, parentPath, requestedName, toType, fromPath);

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

    public List<ResourceResponse> search(Long userId, String query) {
        if (query.trim().isBlank()) {
            throw new InvalidRequestException("Query is empty");
        }
        List<Item> items = fileStorageService.search(userId, query);
        return buildResponse(userId, items);
    }
}
