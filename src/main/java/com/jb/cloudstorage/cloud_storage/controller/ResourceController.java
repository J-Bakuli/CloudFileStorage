package com.jb.cloudstorage.cloud_storage.controller;

import com.jb.cloudstorage.cloud_storage.dto.ResourceResponse;
import com.jb.cloudstorage.cloud_storage.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resource")
@Tag(name = "Resource")
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Operation(summary = "Upload object into the app")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "409", description = "File already exists")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ResourceResponse> upload(
            @RequestParam("path") String path,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        return resourceService.upload(path, file);
    }

    @Operation(summary = "Get resource info")
    @ApiResponse(responseCode = "200", description = "Resource info returned")
    @ApiResponse(responseCode = "400", description = "Invalid or missing path")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "404", description = "Resource is not found")
    @GetMapping
    public ResourceResponse get(
            @RequestParam("path") String path
    ) throws Exception {
        return resourceService.get(path);
    }
}
