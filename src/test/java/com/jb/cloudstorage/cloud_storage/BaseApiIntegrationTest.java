package com.jb.cloudstorage.cloud_storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jb.cloudstorage.cloud_storage.dto.SignUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@SpringBootTest
@Transactional
public abstract class BaseApiIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    private static final String BASIC_USERNAME = "CloudFileStorage";
    private static final String BASIC_PASSWORD = "password123";
    protected MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
    }

    void basicSignUp(MockHttpSession session) throws Exception {
        SignUpRequest signUp = new SignUpRequest(BASIC_USERNAME, BASIC_PASSWORD);
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signUp))
                                .session(session))
                .andExpect(status().isCreated());
    }
}
