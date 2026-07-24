package com.jb.cloudstorage.cloud_storage;

import com.jb.cloudstorage.cloud_storage.dto.SignUpRequest;
import com.jb.cloudstorage.cloud_storage.dto.UserResponse;
import com.jb.cloudstorage.cloud_storage.exception.UsernameAlreadyExistsException;
import com.jb.cloudstorage.cloud_storage.model.UserEntity;
import com.jb.cloudstorage.cloud_storage.repository.UserRepository;
import com.jb.cloudstorage.cloud_storage.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthServiceIntegrationTest extends BaseApiIntegrationTest {
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private MockHttpServletRequest mockRequest;
    private MockHttpServletResponse mockResponse;

    @BeforeEach
    void setUpMocks() {
        mockRequest = new MockHttpServletRequest();
        mockResponse = new MockHttpServletResponse();
    }

    @Test
    void testSignUp_savesUserWithEncodedPassword() {
        SignUpRequest signUpRequest = new SignUpRequest(BASIC_USERNAME, BASIC_PASSWORD);
        UserResponse userResponse = authService.register(signUpRequest, mockRequest, mockResponse);
        UserEntity user = userRepository.findByUsername(BASIC_USERNAME);

        Assertions.assertNotNull(user);
        Assertions.assertTrue(passwordEncoder.matches(BASIC_PASSWORD, user.getPassword()));
        Assertions.assertEquals(BASIC_USERNAME, userResponse.username());
    }

    @Test
    void testSignUp_duplicateUsername() {
        SignUpRequest signUpRequest = new SignUpRequest(BASIC_USERNAME, BASIC_PASSWORD);
        authService.register(signUpRequest, mockRequest, mockResponse);
        Assertions.assertThrows(UsernameAlreadyExistsException.class,
                () -> authService.register(signUpRequest, mockRequest, mockResponse));
    }
}
