package com.jb.cloudstorage.cloud_storage.dto;

import com.jb.cloudstorage.cloud_storage.model.ResourceType;

public record ResourceResponse(
        String path,
        String name,
        Long size,
        ResourceType type) {
}
