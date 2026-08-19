package com.jb.cloudstorage.cloud_storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PathValidationTest extends BaseApiIntegrationTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "../user-1-files/main.txt",
            "folder\\main.txt",
            "folder//main.txt",
            "/main.txt",
            "folder:name/",
            "a*b.txt",
            "x|y.txt"
    })
    void testGetResource_invalidPaths(String path) throws Exception {
        basicSignUp();
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", path)
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid request")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("Invalid path")));
    }

    @Test
    void testResourceEndpoints_pathWithParentTraversal() throws Exception {
        basicSignUp();
        String invalidPath = "../user-1-files/main.txt";

        mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", invalidPath)
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid request")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("Invalid path")));

        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", invalidPath)
                                .param("to", "stolen.txt")
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid request")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("Invalid path")));

        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "secret.txt")
                                .param("to", invalidPath)
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid request")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("Invalid path")));

        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "../user-1-files/secret/")
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid request")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("Invalid path")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../secret",
            "a//b",
            "folder\\x",
            "folder:name/",
            "a*b.txt",
            "x|y.txt"
    })
    void testCreateDirectory_invalid_path(String path) throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", path)
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.errors[*].message", hasItem("Invalid path")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../secret.txt",
            "a//b.txt",
            "folder\\x.txt",
            "folder:name/",
            "a*b.txt",
            "x|y.txt"
    })
    void testMove_invalid_path(String path) throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "secret.txt")
                                .param("to", path)
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid request")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("Invalid path")));
    }
}
