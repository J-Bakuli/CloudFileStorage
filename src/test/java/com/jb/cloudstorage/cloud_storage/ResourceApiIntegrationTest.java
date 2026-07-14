package com.jb.cloudstorage.cloud_storage;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourceApiIntegrationTest extends BaseApiIntegrationTest {
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
    void testGet_unauthenticated() throws Exception {
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "test.txt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGet_notFound() throws Exception {
        basicSignUp(session);
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "noFile.txt")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void testGet_success() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("exam/"))
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.size").value(content.length))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @Test
    void testUploadFile_conflict() throws Exception {
        basicSignUp(session);

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
    }

    @Test
    void testDeleteFile_unauthorized() throws Exception {
        mockMvc.perform(
                        delete("/api/resource")
                                .param("path", "test.txt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDeleteFile_notFound() throws Exception {
        basicSignUp(session);
        mockMvc.perform(
                        delete("/api/resource")
                                .param("path", "noFile.txt")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testDeleteFile_Success() throws Exception {
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
                        delete("/api/resource")
                                .param("path", "exam/test.txt")
                                .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteDirectory_Success() throws Exception {
        basicSignUp(session);
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "newdir/")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("newdir"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));

        mockMvc.perform(
                        delete("/api/resource")
                                .param("path", "newdir/")
                                .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "newdir/")
                                .session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDownloadFile_unauthorized() throws Exception {
        mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", "test.txt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDownloadFile_notFound() throws Exception {
        basicSignUp(session);
        mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", "noFile.txt")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testDownloadFile_success() throws Exception {
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
                        get("/api/resource/download")
                                .param("path", "exam/test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("octet-stream")))
                .andExpect(content().bytes(content));
    }

    @Test
    void testMoveFile_unauthorized() throws Exception {
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "test/test.txt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Disabled
    void testMoveFile_notFound() throws Exception {
        basicSignUp(session);
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "test/test.txt")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    @Disabled
    void testMoveFile_conflict() throws Exception {
        basicSignUp(session);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes()
        );

        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "test")
                                .session(session))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "exam")
                                .session(session))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "test/test.txt")
                                .param("to", "exam/test.txt")
                                .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testMoveFile_move_success() throws Exception {
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
                                .param("path", "test")
                                .session(session))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "test/test.txt")
                                .param("to", "exam/test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(content.length))
                .andExpect(jsonPath("$.path").value("exam/"))
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));

        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "test/test.txt")
                                .session(session))
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"));
    }

    @Test
    void testMoveFile_rename_success() throws Exception {
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
                                .param("path", "test")
                                .session(session))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "test/test.txt")
                                .param("to", "test/new_test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(content.length))
                .andExpect(jsonPath("$.path").value("test/"))
                .andExpect(jsonPath("$.name").value("new_test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));

        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "test/test.txt")
                                .session(session))
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "test/new_test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("new_test.txt"));
    }
}
