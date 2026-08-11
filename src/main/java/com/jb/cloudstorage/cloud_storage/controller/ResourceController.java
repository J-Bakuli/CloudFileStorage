package com.jb.cloudstorage.cloud_storage.controller;

import com.jb.cloudstorage.cloud_storage.dto.ResourceResponse;
import com.jb.cloudstorage.cloud_storage.model.CustomUserDetails;
import com.jb.cloudstorage.cloud_storage.service.ResourceService;
import com.jb.cloudstorage.cloud_storage.util.SafePath;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
@Tag(name = "Resource")
public class ResourceController {
    private final ResourceService resourceService;

    @Operation(summary = "Upload object into the app")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "409", description = "File already exists")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ResourceResponse> upload(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("path") @SafePath String path,
            @RequestParam("object") List<MultipartFile> objects
    ) {
        return resourceService.upload(user.getId(), path, objects);
    }

    @Operation(summary = "Get resource info")
    @ApiResponse(responseCode = "200", description = "Resource info returned")
    @ApiResponse(responseCode = "400", description = "Invalid or missing path")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "404", description = "Resource is not found")
    @GetMapping
    public ResourceResponse get(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("path") @SafePath String path
    ) {
        return resourceService.get(user.getId(), path);
    }

    @Operation(summary = "Delete resource")
    @ApiResponse(responseCode = "204", description = "Success, no content")
    @ApiResponse(responseCode = "400", description = "Invalid or missing path")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "404", description = "Resource is not found")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("path") @SafePath String path
    ) {
        resourceService.delete(user.getId(), path);
    }

    @Operation(summary = "Download resource")
    @ApiResponse(responseCode = "200", description = "Binary content: application/octet-stream for file, application/zip for directory")
    @ApiResponse(responseCode = "400", description = "Invalid or missing path")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "404", description = "Resource is not found")
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("path") @SafePath String path
    ) {
        return resourceService.download(user.getId(), path);
    }

    @Operation(summary = "Move resource: rename or move")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "400", description = "Invalid or missing path")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "404", description = "Resource is not found")
    @ApiResponse(responseCode = "409", description = "Resource from path to already exists")
    @PostMapping("/move")
    public ResourceResponse move(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("from") @SafePath String fromPath, @RequestParam("to") @SafePath String toPath
    ) {
        return resourceService.move(user.getId(), fromPath, toPath);
    }

    @Operation(summary = "Search resource")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "400", description = "Invalid or missing query")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @GetMapping("/search")
    public List<ResourceResponse> search(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("query") String query
    ) {
        return resourceService.search(user.getId(), query);
    }
}
