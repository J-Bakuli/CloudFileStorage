package com.jb.cloudstorage.cloud_storage.controller;

import com.jb.cloudstorage.cloud_storage.dto.SignInRequest;
import com.jb.cloudstorage.cloud_storage.dto.SignUpRequest;
import com.jb.cloudstorage.cloud_storage.dto.UserResponse;
import com.jb.cloudstorage.cloud_storage.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "A new user registration")
    @ApiResponse(responseCode = "201", description = "User is created")
    @ApiResponse(responseCode = "400", description = "Validation exception")
    @ApiResponse(responseCode = "409", description = "Username is already taken")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/sign-up")
    public UserResponse register(
            @Valid @RequestBody SignUpRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        return authService.register(request, httpServletRequest, httpServletResponse);
    }

    @Operation(summary = "Sign-in of the already registered user")
    @ApiResponse(responseCode = "200", description = "Success, session is set")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/sign-in")
    public UserResponse login(
            @Valid @RequestBody SignInRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        return authService.login(request, httpServletRequest, httpServletResponse);
    }

    @Operation(summary = "Sign-out from the app")
    @ApiResponse(responseCode = "204", description = "Success, no content")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/sign-out")
    public void logout(HttpServletRequest httpServletRequest) {
        authService.logout(httpServletRequest);
    }
}
