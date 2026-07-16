package com.jb.cloudstorage.cloud_storage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jb.cloudstorage.cloud_storage.model.ResourceType;

public record ResourceResponse(
        String path,
        String name,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Long size,
        ResourceType type) {
}
