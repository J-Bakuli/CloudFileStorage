package com.jb.cloudstorage.cloud_storage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DirectoryApiIntegrationTest extends BaseApiIntegrationTest {
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
    void testGetDirectory_unauthenticated() throws Exception {
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "exam/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetDirectory_notFound() throws Exception {
        basicSignUp(session);
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "missing/")
                                .session(session))
                .andExpect(status().isNotFound());
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

    @Test
    void testCreateDirectory_parentNotFound() throws Exception {
        basicSignUp(session);
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "missing/new-dir")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testCreateDirectory_conflict() throws Exception {
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
                        post("/api/directory")
                                .param("path", "newdir")
                                .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testDownloadDirectory_notFound() throws Exception {
        basicSignUp(session);
        mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", "missing/")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Disabled
    @Test
    void testDownloadDirectory_success() throws Exception {
        basicSignUp(session);
        byte[] content = "hello".getBytes();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                content
        );

        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "newdir")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("newdir"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));

        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "newdir/")
                                .session(session))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", "newdir/")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/zip")))
                .andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsByteArray().length > 0);
    }
}
