package com.jb.cloudstorage.cloud_storage.service;

import com.jb.cloudstorage.cloud_storage.dto.ResourceResponse;
import com.jb.cloudstorage.cloud_storage.model.ResourceType;
import com.jb.cloudstorage.cloud_storage.model.UserEntity;
import com.jb.cloudstorage.cloud_storage.repository.UserRepository;
import com.jb.cloudstorage.cloud_storage.util.FileUtils;
import io.minio.messages.Item;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username);
        Long userId = user.getId();

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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username);
        Long userId = user.getId();
        List<Item> objects = fileStorageService.listObjects(userId, fullPath);
        return buildResponse(userId, objects);
    }

    public List<ResourceResponse> upload(String folderPath, MultipartFile file) throws Exception { //Todo remove throwing Exception later
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username);
        Long userId = user.getId();
        fileStorageService.uploadFile(userId, folderPath, file);
        return List.of(buildResponse(folderPath, file));
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
