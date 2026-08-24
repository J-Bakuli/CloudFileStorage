package com.jb.cloudstorage.cloud_storage;

import com.jb.cloudstorage.cloud_storage.dto.SignInRequest;
import com.jb.cloudstorage.cloud_storage.dto.SignUpRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthApiIntegrationTest extends BaseApiIntegrationTest {
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
    void testSignUp_signIn_trimUserName() throws Exception {
        String userName = "AlexLivitskiy";
        String userNameToTrim = " AlexLivitskiy ";
        SignUpRequest request0 = new SignUpRequest(userName, BASIC_PASSWORD);
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request0)))
                .andExpect(status().isCreated());

        SignUpRequest request1 = new SignUpRequest(userNameToTrim, BASIC_PASSWORD);
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isConflict());

        SignInRequest request2 = new SignInRequest(userNameToTrim, BASIC_PASSWORD);
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(userName)));
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
                .andExpect(jsonPath("$.message", containsString("already exists")));
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
        basicSignUpAndSessionCookie();
        basicSignIn();
    }

    @Test
    void testAuthenticatedPrincipal_success() throws Exception {
        basicSignUpAndSessionCookie();
        Cookie cookie = basicSignIn();
        uploadBasicFile(cookie);
        basicSignOut(cookie);

        String username = "AlexanderPushkin";
        String password = "%shsHjsn45S";
        SignUpRequest signUpRequest = new SignUpRequest(username, password);
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is(username)));

        SignInRequest signInRequest = new SignInRequest(username, password);
        MvcResult signInResult = mockMvc.perform(
                        post("/api/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isOk())
                .andReturn();
        cookie = getCookieFromMvcResult(signInResult);
        mockMvc.perform(
                        get("/api/user/me")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(username)));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
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
                .andExpect(jsonPath("$.message", is("Unauthorized request")));
    }

    @Test
    void testSignIn_wrongPassword() throws Exception {
        basicSignUpAndSessionCookie();
        String password = "AAAAAAAAAAAA";
        SignInRequest signInRequest = new SignInRequest(BASIC_USERNAME, password);
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Unauthorized request")));
    }

    @Test
    void testSignIn_emptyBody_badRequest() throws Exception {
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid request body")));
    }

    @Test
    void testSignIn_malformedJson_badRequest() throws Exception {
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid request body")));
    }

    @Test
    void testSignOut_UserMe_success() throws Exception {
        basicSignUpAndSessionCookie();
        Cookie cookie = basicSignIn();
        basicGetUserMe(cookie);
        basicSignOut(cookie);
        mockMvc.perform(
                        get("/api/user/me")
                                .cookie(cookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized request"))
                .andExpect(jsonPath("$.path").value("/api/user/me"));
    }

    @Test
    void testSignOut_unauthorized() throws Exception {
        basicSignUpAndSessionCookie();
        Cookie cookie = basicSignIn();
        basicGetUserMe(cookie);
        basicSignOut(cookie);
        mockMvc.perform(
                        post("/api/auth/sign-out")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testUserMe_unauthenticated() throws Exception {
        mockMvc.perform(
                        get("/api/user/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized request"))
                .andExpect(jsonPath("$.path").value("/api/user/me"));
    }
}
