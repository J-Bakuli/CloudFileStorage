package com.jb.cloudstorage.cloud_storage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
        basicSignUp();
        uploadBasicFile();
        getBasicFile();
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized request"))
                .andExpect(jsonPath("$.path").value("/api/directory"));
    }

    @Test
    void testGetDirectory_notFound() throws Exception {
        basicSignUp();
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized request"))
                .andExpect(jsonPath("$.path").value("/api/directory"));
    }

    @Test
    void testCreateDirectory_success() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "newdir")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("newdir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("newdir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$[0].type").value("DIRECTORY"));
    }

    @Test
    void testCreateDirectory_one_directory_per_time_success() throws Exception {
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
                        get("/api/directory")
                                .param("path", "")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].path").isEmpty())
                .andExpect(jsonPath("$[0].name").value("level1"))
                .andExpect(jsonPath("$[0].size").doesNotExist())
                .andExpect(jsonPath("$[0].type").value("DIRECTORY"));
    }

    @Test
    void testCreateDirectory_several_directories_per_time_parentNotFound() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "level1/level2/")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testCreateDirectory_sameParent_caseInsensitive_conflict() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("projects"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/newdir/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/ NEWdir /")
                                .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testCreateDirectory_sameNameDifferentParent_success() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/newdir/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "archive/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "archive/NEWDIR")
                                .session(session))
                .andExpect(status().isCreated());
    }

    @Test
    void testDownloadDirectory_notFound() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", "missing/")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testDownloadDirectory_notFound_acceptOctetStream() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", "missing/")
                                .accept(MediaType.APPLICATION_OCTET_STREAM)
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testDownloadDirectory_success() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "newdir")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("newdir"))
                .andExpect(jsonPath("$.size").doesNotExist())
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
                .andExpect(jsonPath("$.size").doesNotExist())
                .andReturn();
        Assertions.assertTrue(result.getResponse().getContentAsByteArray().length > 0);
    }

    @Test
    void testMoveDirectory_unauthorized() throws Exception {
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/")
                                .param("to", "test/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized request"))
                .andExpect(jsonPath("$.path").value("/api/resource/move"));
    }

    @Test
    void testMoveDirectory_notFound() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/")
                                .param("to", "test/")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testMoveDirectory_conflict() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("dir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "newdir")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("newdir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", "newdir/")
                                .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testMoveDirectory_conflict_caseInsensitiveName() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("dir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "newdir")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("newdir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", " NEWDIR /")
                                .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", " DIR /")
                                .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testMoveDirectory_sameNameDifferentParent_caseInsensitive_success() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/newdir/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "archive/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", "archive/NEWDIR/")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("archive/"))
                .andExpect(jsonPath("$.name").value("NEWDIR"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
    }

    @Test
    void testMoveDirectory_sameNameAsFile_differentType_success() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", "TEST.TXT/")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("TEST.TXT"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
    }

    @Test
    void testMoveDirectory_moveParentToChild_badRequest() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam1/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam1/exam2/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam1/")
                                .param("to", "exam1/exam2/exam1/")
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid operation request, cannot move")));
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "exam1/")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("exam2"))
                .andExpect(jsonPath("$[0].type").value("DIRECTORY"));
    }

    @Test
    void testMoveDirectory_samePath_badRequest() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam1/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam1/")
                                .param("to", "exam1/")
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid operation request, cannot move")));
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("exam1"))
                .andExpect(jsonPath("$[0].type").value("DIRECTORY"));
    }

    @Test
    void testMoveDirectory_move_success() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("dir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "dir")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", "exam/")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "dir/test.txt")
                                .session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "dir/")
                                .session(session))
                .andExpect(status().isNotFound());
        getBasicFile();
    }

    @Test
    void testMoveDirectory_rename_success() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("dir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "dir")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", "exam/")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "dir/test.txt")
                                .session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "dir/")
                                .session(session))
                .andExpect(status().isNotFound());
        getBasicFile();
    }

    @Test
    void testMoveDirectory_intoParent_success() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "other")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("other"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir")
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("dir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "dir")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", "other/dir/")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("other/"))
                .andExpect(jsonPath("$.name").value("dir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "dir/test.txt")
                                .session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "dir/")
                                .session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "other/dir/test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("other/dir/"))
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
    }
}
