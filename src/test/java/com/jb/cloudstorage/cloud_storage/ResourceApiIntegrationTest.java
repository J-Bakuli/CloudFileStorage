package com.jb.cloudstorage.cloud_storage;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourceApiIntegrationTest extends BaseApiIntegrationTest {
    @Test
    void testUpload_unauthenticated() throws Exception {
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
        basicSignUp();
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "noFile.txt")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void testGet_success() throws Exception {
        basicSignUp();
        uploadBasicFile();
        getBasicFile();
    }

    @Test
    void testUploadFile_conflict() throws Exception {
        basicSignUp();
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
    @Disabled
    void testUploadFile_conflict_caseInsensitiveName() throws Exception {
        basicSignUp();
        MockMultipartFile upperCaseNameFile = new MockMultipartFile(
                "object",
                "TEST.TXT",
                MediaType.TEXT_PLAIN_VALUE,
                content
        );
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(upperCaseNameFile)
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
        basicSignUp();
        mockMvc.perform(
                        delete("/api/resource")
                                .param("path", "noFile.txt")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testDeleteFile_Success() throws Exception {
        basicSignUp();
        uploadBasicFile();
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
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "newdir/")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("newdir"))
                .andExpect(jsonPath("$.size").doesNotExist())
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
        basicSignUp();
        mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", "noFile.txt")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testDownloadFile_success() throws Exception {
        basicSignUp();
        uploadBasicFile();
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
    void testMoveFile_notFound() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "test/test.txt")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testMoveFile_conflict() throws Exception {
        basicSignUp();
        uploadBasicFile();
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @Disabled
    void testMoveFile_conflict_caseInsensitiveName() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "test")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "test/test.txt")
                                .param("to", "exam/TEST.TXT")
                                .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testMoveFile_move_success() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "exam1/test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(content.length))
                .andExpect(jsonPath("$.path").value("exam1/"))
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam1/test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"));
    }

    @Test
    void testMoveFile_rename_success() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "exam/new_test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(content.length))
                .andExpect(jsonPath("$.path").value("exam/"))
                .andExpect(jsonPath("$.name").value("new_test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/new_test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("new_test.txt"));
    }


    @Test
    void testSearch_unauthenticated() throws Exception {
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "timelines"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testSearch_notFound() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "timelines")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(0)))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void testSearch_blankQuery() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "")
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Query is empty")));
    }

    @Test
    void testSearch_success_searchFile_trimAndPartialName() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value("exam/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(content.length))
                .andExpect(jsonPath("$[0].type").value("FILE"));
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "tes")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value("exam/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(content.length))
                .andExpect(jsonPath("$[0].type").value("FILE"));
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "EXam")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value("exam/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(content.length))
                .andExpect(jsonPath("$[0].type").value("FILE"));
    }

    @Test
    void testSearch_success_searchFile_with_same_fileName_in_several_folders() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "daily")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].path").value("daily/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(content.length))
                .andExpect(jsonPath("$[0].type").value("FILE"))
                .andExpect(jsonPath("$[1].path").value("exam/"))
                .andExpect(jsonPath("$[1].name").value("test.txt"))
                .andExpect(jsonPath("$[1].size").value(content.length))
                .andExpect(jsonPath("$[1].type").value("FILE"));
    }

    @Test
    void testSearch_success_searchFile_in_nested_directories() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "level1/level2/level3")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value("level1/level2/level3/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(content.length))
                .andExpect(jsonPath("$[0].type").value("FILE"));
    }

    @Test
    void testSearch_success_searchDirectory_without_file() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "level1/")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("level1"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "level1")
                                .session(session))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$[0].type").value("DIRECTORY"));
    }

    @Test
    void testSearch_success_searchDirectory_with_one_directory_per_time_creation_without_file() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "level1/")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("level1"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "level1/level2/")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value("level1/"))
                .andExpect(jsonPath("$.name").value("level2"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "level1/level2/level3/")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value("level1/level2/"))
                .andExpect(jsonPath("$.name").value("level3"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "level1/level2")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].path").value("level1/"))
                .andExpect(jsonPath("$[0].name").value("level2"))
                .andExpect(jsonPath("$[0].size").doesNotExist())
                .andExpect(jsonPath("$[0].type").value("DIRECTORY"))
                .andExpect(jsonPath("$[1].path").value("level1/level2/"))
                .andExpect(jsonPath("$[1].name").value("level3"))
                .andExpect(jsonPath("$[1].size").doesNotExist())
                .andExpect(jsonPath("$[1].type").value("DIRECTORY"));
    }

    @Test
    void testSearch_success_explicit_dir_creation_with_file() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam/")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        uploadBasicFile();
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "exam")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].path").value("exam/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(content.length))
                .andExpect(jsonPath("$[0].type").value("FILE"))
                .andExpect(jsonPath("$[1].path").value(""))
                .andExpect(jsonPath("$[1].name").value("exam"))
                .andExpect(jsonPath("$[1].size").doesNotExist())
                .andExpect(jsonPath("$[1].type").value("DIRECTORY"));
    }

    @Test
    void testSearch_success_without_explicit_dir_creation_with_file() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "exam")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value("exam/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(content.length))
                .andExpect(jsonPath("$[0].type").value("FILE"));
    }
}
