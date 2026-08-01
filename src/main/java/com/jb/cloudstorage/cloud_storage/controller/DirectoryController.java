package com.jb.cloudstorage.cloud_storage.controller;

import com.jb.cloudstorage.cloud_storage.dto.ResourceResponse;
import com.jb.cloudstorage.cloud_storage.service.ResourceService;
import com.jb.cloudstorage.cloud_storage.util.SafePath;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/directory")
@Tag(name = "Directory")
public class DirectoryController {
    private final ResourceService resourceService;
    public DirectoryController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Operation(summary = "Get directory content")
    @ApiResponse(responseCode = "200", description = "Directory content returned")
    @ApiResponse(responseCode = "400", description = "Invalid or missing path")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "404", description = "Resource is not found")
    @GetMapping
    public List<ResourceResponse> getDirectory(
            @RequestParam("path") @SafePath String path
    ) {
        return resourceService.getDirectory(path);
    }

    @Operation(summary = "Create empty directory")
    @ApiResponse(responseCode = "201", description = "Directory is created")
    @ApiResponse(responseCode = "409", description = "Directory already exists")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "404", description = "Parent path does not exist")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceResponse createDirectory(
            @RequestParam("path") @SafePath String path
    ) {
        return resourceService.createDirectory(path);
    }
}
