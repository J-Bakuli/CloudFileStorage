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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
@Transactional
class AuthServiceIntegrationTest {
    @ServiceConnection
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");
    @Autowired
    AuthService authService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private static final String username = "TestCloud";
    private static final String rawPassword = "password123";

    private MockHttpServletRequest mockRequest;
    private MockHttpServletResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockRequest = new MockHttpServletRequest();
        mockResponse = new MockHttpServletResponse();
    }

    @Test
    void testSignUp_savesUserWithEncodedPassword() {
        SignUpRequest signUpRequest = new SignUpRequest(username, rawPassword);
        UserResponse userResponse = authService.register(signUpRequest, mockRequest, mockResponse);
        UserEntity user = userRepository.findByUsername(username);

        Assertions.assertNotNull(user);
        Assertions.assertTrue(passwordEncoder.matches(rawPassword, user.getPassword()));
        Assertions.assertEquals(username, userResponse.username());
    }

    @Test
    void testSignUp_duplicateUsername() {
        SignUpRequest signUpRequest = new SignUpRequest(username, rawPassword);
        authService.register(signUpRequest, mockRequest, mockResponse);
        Assertions.assertThrows(UsernameAlreadyExistsException.class,
                () -> authService.register(signUpRequest, mockRequest, mockResponse));
    }
}
