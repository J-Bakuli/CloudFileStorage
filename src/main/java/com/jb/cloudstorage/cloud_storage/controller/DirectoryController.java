package com.jb.cloudstorage.cloud_storage.controller;

import com.jb.cloudstorage.cloud_storage.dto.ResourceResponse;
import com.jb.cloudstorage.cloud_storage.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/directory")
@Tag(name = "Directory")
public class DirectoryController {
    private final ResourceService resourceService;

    public DirectoryController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Operation(summary = "Get directory info")
    @ApiResponse(responseCode = "400", description = "Invalid or missing path")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "404", description = "Resource is not found")
    @GetMapping
    public List<ResourceResponse> getDirectory(
            @RequestParam("path") String path
    ) throws Exception {
        return resourceService.getDirectory(path);
    }
}
