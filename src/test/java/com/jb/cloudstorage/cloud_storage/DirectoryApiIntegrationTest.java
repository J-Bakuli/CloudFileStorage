package com.jb.cloudstorage.cloud_storage;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DirectoryApiIntegrationTest extends BaseApiIntegrationTest{
    @Test
    void testGetDirectory_success() throws Exception {
        basicSignUp(session);
        byte[] content = "hello".getBytes();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                content
        );

        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "exam")
                                .session(session))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "exam/")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].path").value("exam/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(content.length))
                .andExpect(jsonPath("$[0].type").value("FILE"));
    }

    @Test
    void testCreateDirectory_unauthenticated() throws Exception {
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateDirectory_success() throws Exception {
        basicSignUp(session);
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "newdir")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("newdir"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));

        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("newdir"))
                .andExpect(jsonPath("$[0].type").value("DIRECTORY"));
    }
}
