package com.jb.cloudstorage.cloud_storage.controller;

import com.jb.cloudstorage.cloud_storage.dto.SignInRequest;
import com.jb.cloudstorage.cloud_storage.dto.SignUpRequest;
import com.jb.cloudstorage.cloud_storage.dto.UserResponse;
import com.jb.cloudstorage.cloud_storage.service.AuthService;
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
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/sign-up")
    public UserResponse register(
            @Valid @RequestBody SignUpRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        return authService.register(request, httpServletRequest, httpServletResponse);
    }

    @PostMapping("/sign-in")
    public UserResponse login(
            @Valid @RequestBody SignInRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        return authService.login(request, httpServletRequest, httpServletResponse);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/sign-out")
    public void logout(HttpServletRequest httpServletRequest) {
        authService.logout(httpServletRequest);
    }
}
