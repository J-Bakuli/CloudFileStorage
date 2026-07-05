package com.jb.cloudstorage.cloud_storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jb.cloudstorage.cloud_storage.dto.SignUpRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@SpringBootTest
@Transactional
public class ResourceApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @ServiceConnection
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");
    private static final String BASIC_USERNAME = "CloudFileStorage";
    private static final String BASIC_PASSWORD = "password123";

    @Test
    void testUpload_unauthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes());
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", ""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUploadFile_conflict() throws Exception {
        MockHttpSession session = new MockHttpSession();
        SignUpRequest signUp = new SignUpRequest(BASIC_USERNAME, BASIC_PASSWORD);

        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signUp))
                                .session(session))
                .andExpect(status().isCreated());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes()
        );

        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "")
                                .session(session))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "")
                                .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    } //Todo test fails now, TDD, check later
}
