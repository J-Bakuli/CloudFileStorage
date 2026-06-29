package com.jb.cloudstorage.cloud_storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank
        @Size(min = 5, max = 20, message = "Username is to contain from 5 to 20 characters")
        String username,
        @NotBlank
        @Size(min = 5, max = 20, message = "Password is to contain from 5 to 20 characters")
        String password) {
}
