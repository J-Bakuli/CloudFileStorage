package com.jb.cloudstorage.cloud_storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jb.cloudstorage.cloud_storage.dto.SignInRequest;
import com.jb.cloudstorage.cloud_storage.dto.SignUpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@SpringBootTest
@Transactional
public class AuthApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @ServiceConnection
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");
    private static final String BASIC_USERNAME = "CloudFileStorage";
    private static final String BASIC_PASSWORD = "password123";

    @ParameterizedTest
    @CsvSource({
            "AAA, username, 5 to 20",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA, username, 5 to 20",
            ", username, must not be blank"
    })
    void testSignUp_invalidUsername(String username, String expectedField, String expectedMessagePart) throws Exception {
        SignUpRequest request = new SignUpRequest(username, BASIC_PASSWORD);
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", containsString(expectedMessagePart)))
                .andExpect(jsonPath("$.errors[0].field", is(expectedField)));
    }

    @ParameterizedTest
    @CsvSource({
            "pas, password, 5 to 20",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA, password, 5 to 20",
            ", password, must not be blank"
    })
    void testSignUp_invalidPassword(String password, String expectedField, String expectedMessagePart) throws Exception {
        SignUpRequest request = new SignUpRequest(BASIC_USERNAME, password);
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", containsString(expectedMessagePart)))
                .andExpect(jsonPath("$.errors[0].field", is(expectedField)));
    }

    @Test
    void testSignUp_success() throws Exception {
        SignUpRequest request = new SignUpRequest(BASIC_USERNAME, BASIC_PASSWORD);
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is(BASIC_USERNAME)));
    }

    @Test
    void testSignUp_conflict() throws Exception {
        SignUpRequest request = new SignUpRequest(BASIC_USERNAME, BASIC_PASSWORD);
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")))
        ;
    }

    @ParameterizedTest
    @CsvSource({
            "AAA, username, 5 to 20",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA, username, 5 to 20",
            ", username, must not be blank"
    })
    void testSignIn_invalidUsername(String username, String expectedField, String expectedMessagePart) throws Exception {
        SignInRequest request = new SignInRequest(username, BASIC_PASSWORD);
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", containsString(expectedMessagePart)))
                .andExpect(jsonPath("$.errors[0].field", is(expectedField)));
    }

    @ParameterizedTest
    @CsvSource({
            "AAA, password, 5 to 20",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA, password, 5 to 20",
            ", password, must not be blank"
    })
    void testSignIn_invalidPassword(String password, String expectedField, String expectedMessagePart) throws Exception {
        SignInRequest request = new SignInRequest(BASIC_USERNAME, password);
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", containsString(expectedMessagePart)))
                .andExpect(jsonPath("$.errors[0].field", is(expectedField)));
    }

    @Test
    void testSignIn_success() throws Exception {
        MockHttpSession session = new MockHttpSession();
        basicSignUp(session);
        basicSignIn(session);
    }

    @Test
    void testSignIn_unknownUsername() throws Exception {
        String username = "UnknownUser";
        SignInRequest signInRequest = new SignInRequest(username, BASIC_PASSWORD);
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid username or password")));
    }

    @Test
    void testSignIn_wrongPassword() throws Exception {
        MockHttpSession session = new MockHttpSession();
        basicSignUp(session);

        String password = "AAAAAAAAAAAA";
        SignInRequest signInRequest = new SignInRequest(BASIC_USERNAME, password);
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid username or password")));
    }

    @Test
    void testSignOut_UserMe_success() throws Exception {
        MockHttpSession session = new MockHttpSession();
        basicSignUp(session);
        basicSignIn(session);

        mockMvc.perform(
                        get("/api/user/me")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(BASIC_USERNAME)));

        mockMvc.perform(
                        post("/api/auth/sign-out")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/user/me")
                                .session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUserMe_unauthenticated() throws Exception {
        mockMvc.perform(
                        get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

    private void basicSignUp(MockHttpSession session) throws Exception {
        SignUpRequest signUpRequest = new SignUpRequest(BASIC_USERNAME, BASIC_PASSWORD);
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is(BASIC_USERNAME)));
        ;
    }

    private void basicSignIn(MockHttpSession session) throws Exception {
        SignInRequest signInRequest = new SignInRequest(BASIC_USERNAME, BASIC_PASSWORD);
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(BASIC_USERNAME)));
        ;
    }
}
