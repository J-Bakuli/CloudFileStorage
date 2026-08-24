package com.jb.cloudstorage.cloud_storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jb.cloudstorage.cloud_storage.dto.SignInRequest;
import com.jb.cloudstorage.cloud_storage.dto.SignUpRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
public abstract class BaseApiIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    protected static final String BASIC_USERNAME = "CloudFileStorage";
    protected static final String BASIC_PASSWORD = "password123";
    protected final byte[] content = "hello".getBytes();
    protected final MockMultipartFile file = new MockMultipartFile(
            "object",
            "test.txt",
            MediaType.TEXT_PLAIN_VALUE,
            content
    );

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static MinIOContainer minio = new MinIOContainer("minio/minio:latest")
            .withUserName("test")
            .withPassword("test_password");

    static GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:latest"))
            .withExposedPorts(6379);

    static {
        postgres.start();
        minio.start();
        redis.start();
    }

    @DynamicPropertySource
    static void s3Properties(DynamicPropertyRegistry registry) {
        registry.add("minio.endpoint", () -> minio.getS3URL());
        registry.add("minio.access-key", minio::getUserName);
        registry.add("minio.secret-key", minio::getPassword);
        registry.add("minio.bucket", () -> "user-files");
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.data.redis.password", () -> "");
    }

    Cookie basicSignUpAndSessionCookie() throws Exception {
        SignUpRequest signUp = new SignUpRequest(BASIC_USERNAME, BASIC_PASSWORD);
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signUp)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is(BASIC_USERNAME)))
                .andReturn();
        return getCookieFromMvcResult(mvcResult);
    }

    Cookie basicSignIn() throws Exception {
        SignInRequest signInRequest = new SignInRequest(BASIC_USERNAME, BASIC_PASSWORD);
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(BASIC_USERNAME)))
                .andReturn();
        return getCookieFromMvcResult(mvcResult);
    }

    void uploadBasicFile(Cookie cookie) throws Exception {
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "exam/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
    }

    void basicSignOut(Cookie cookie) throws Exception {
        mockMvc.perform(
                        post("/api/auth/sign-out")
                                .cookie(cookie)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    void basicGetUserMe(Cookie cookie) throws Exception {
        mockMvc.perform(
                        get("/api/user/me")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(BASIC_USERNAME)));
    }

    void getBasicFile(Cookie cookie) throws Exception {
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("exam/"))
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.size").value(content.length))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    protected Cookie getCookieFromMvcResult(MvcResult mvcResult) {
        Cookie cookie = mvcResult.getResponse().getCookie("SESSION");
        Assertions.assertNotNull(cookie, "SESSION cookie missing");
        return cookie;
    }
}
