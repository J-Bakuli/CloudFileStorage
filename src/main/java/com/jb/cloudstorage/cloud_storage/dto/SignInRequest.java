package com.jb.cloudstorage.cloud_storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignInRequest(
        @NotBlank
        @Size(min = 5, max = 20, message = "Username is to contain from 5 to 20 characters")
        @Pattern(
                regexp = "^[a-zA-Z0-9]+[a-zA-Z_0-9]*[a-zA-Z0-9]+$",
                message = "Username contains invalid characters"
        )
        String username,
        @NotBlank
        @Size(min = 5, max = 20, message = "Password is to contain from 5 to 20 characters")
        @Pattern(
                regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>\\[\\]\\/`~+=\\-_';]*$",
                message = "Password contains invalid characters"
        )
        String password) {
        public SignInRequest {
                if (username != null) {
                        username = username.trim();
                }
        }
}
