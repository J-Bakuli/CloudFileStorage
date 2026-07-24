package com.jb.cloudstorage.cloud_storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jb.cloudstorage.cloud_storage.dto.SignInRequest;
import com.jb.cloudstorage.cloud_storage.dto.SignUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@SpringBootTest
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class})
@Transactional
public abstract class BaseApiIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    protected static final String BASIC_USERNAME = "CloudFileStorage";
    protected static final String BASIC_PASSWORD = "password123";
    protected MockHttpSession session;
    protected final byte[] content = "hello".getBytes();
    protected final MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            MediaType.TEXT_PLAIN_VALUE,
            content
    );

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
    }

    void basicSignUp() throws Exception {
        SignUpRequest signUp = new SignUpRequest(BASIC_USERNAME, BASIC_PASSWORD);
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signUp))
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is(BASIC_USERNAME)));
    }

    void basicSignIn() throws Exception {
        SignInRequest signInRequest = new SignInRequest(BASIC_USERNAME, BASIC_PASSWORD);
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(BASIC_USERNAME)));
    }

    void uploadBasicFile() throws Exception {
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "exam")
                                .session(session))
                .andExpect(status().isCreated());
    }

    void getBasicFile() throws Exception {
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("exam/"))
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.size").value(content.length))
                .andExpect(jsonPath("$.type").value("FILE"));
    }
}
