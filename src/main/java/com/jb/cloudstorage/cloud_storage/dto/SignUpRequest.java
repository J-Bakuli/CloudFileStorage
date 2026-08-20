package com.jb.cloudstorage.cloud_storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank
        @Size(min = 5, max = 20, message = "Username must contain from 5 to 20 characters")
        @Pattern(
                regexp = "^[a-zA-Z0-9]+[a-zA-Z_0-9]*[a-zA-Z0-9]+$",
                message = "Username contains invalid characters"
        )
        String username,
        @NotBlank
        @Size(min = 5, max = 20, message = "Password must contain from 5 to 20 characters")
        @Pattern(
                regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>\\[\\]\\/`~+=\\-_';]+$",
                message = "Password contains invalid characters"
        )
        String password) {
        public SignUpRequest {
                if (username != null) {
                        username = username.trim();
                }
        }
}
