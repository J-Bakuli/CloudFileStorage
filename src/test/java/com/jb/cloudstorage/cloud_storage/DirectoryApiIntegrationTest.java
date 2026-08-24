package com.jb.cloudstorage.cloud_storage;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DirectoryApiIntegrationTest extends BaseApiIntegrationTest {
    @Test
    void testGetDirectory_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        getBasicFile(cookie);
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "exam/")
                                .cookie(cookie))
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
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "missing/")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetDirectory_invalidPath() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "/")
                                .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.errors[*].message", hasItem("Invalid path")));
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "exam")
                                .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Directory path must end with /, path=exam"));
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
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "newdir/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("newdir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("newdir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$[0].type").value("DIRECTORY"));
    }

    @Test
    void testCreateDirectory_invalidPath() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "newdir")
                                .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Directory path must end with /, path=newdir"));
    }

    @Test
    void testCreateDirectory_one_directory_per_time_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "level1/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("level1"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "level1/level2/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value("level1/"))
                .andExpect(jsonPath("$.name").value("level2"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "level1/level2/level3/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value("level1/level2/"))
                .andExpect(jsonPath("$.name").value("level3"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "level1/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].path").value("level1/"))
                .andExpect(jsonPath("$[0].name").value("level2"))
                .andExpect(jsonPath("$[0].size").doesNotExist())
                .andExpect(jsonPath("$[0].type").value("DIRECTORY"));
    }

    @Test
    void testCreateDirectory_several_directories_per_time_parentNotFound() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "level1/level2/")
                                .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testCreateDirectory_sameParent_caseInsensitive_conflict() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("projects"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/newdir/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/NEWdir/")
                                .cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testCreateDirectory_sameNameDifferentParent_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/newdir/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "archive/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "archive/NEWDIR/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
    }

    @Test
    void testDownloadDirectory_notFound() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", "missing/")
                                .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testDownloadDirectory_notFound_acceptOctetStream() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", "missing/")
                                .accept(MediaType.APPLICATION_OCTET_STREAM)
                                .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testDownloadDirectory_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "newdir/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("newdir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "newdir/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", "newdir/")
                                .cookie(cookie))
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
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/")
                                .param("to", "test/")
                                .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testMoveDirectory_resource_type_change() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam1/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam1/")
                                .param("to", "exam1")
                                .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid operation request")));
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("exam1"))
                .andExpect(jsonPath("$[0].type").value("DIRECTORY"));
    }

    @Test
    void testMoveDirectory_conflict() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("dir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "newdir/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("newdir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", "newdir/")
                                .cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testMoveDirectory_conflict_caseInsensitiveName() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("dir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "newdir/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("newdir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", "NEWDIR/")
                                .cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testMoveDirectory_sameNameDifferentParent_caseInsensitive_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/newdir/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "archive/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", "archive/NEWDIR/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("archive/"))
                .andExpect(jsonPath("$.name").value("NEWDIR"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
    }

    @Test
    void testMoveDirectory_sameNameAsFile_differentType_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", "TEST.TXT/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("TEST.TXT"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
    }

    @Test
    void testMoveDirectory_moveParentToChild_badRequest() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam1/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam1/exam2/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam1/")
                                .param("to", "exam1/exam2/exam1/")
                                .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid operation request, cannot move")));
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "exam1/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("exam2"))
                .andExpect(jsonPath("$[0].type").value("DIRECTORY"));
    }

    @Test
    void testMoveDirectory_samePath_badRequest() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam1/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam1/")
                                .param("to", "exam1/")
                                .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid operation request, cannot move")));
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("exam1"))
                .andExpect(jsonPath("$[0].type").value("DIRECTORY"));
    }

    @Test
    void testMoveDirectory_move_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("dir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "dir/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", "exam/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "dir/test.txt")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "dir/")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
        getBasicFile(cookie);
    }

    @Test
    void testMoveDirectory_rename_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("dir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "dir/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", "exam/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "dir/test.txt")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "dir/")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
        getBasicFile(cookie);
    }

    @Test
    void testMoveDirectory_rename_similarPrefix_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam1/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam1/")
                                .param("to", "exam12/")
                                .cookie(cookie))
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "exam12/")
                                .cookie(cookie))
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "exam1/")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void testMoveDirectory_rename_caseInsensitiveRename() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam1/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam1/")
                                .param("to", "eXAm1/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("eXAm1"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "exam1/")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "eXAm1/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("eXAm1"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
    }

    @Test
    void testMoveDirectory_intoParent_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "other/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("other"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "dir/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("dir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "dir/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "dir/")
                                .param("to", "other/dir/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("other/"))
                .andExpect(jsonPath("$.name").value("dir"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "dir/test.txt")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "dir/")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "other/dir/test.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("other/dir/"))
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @Test
    void testDirectoryMerge_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "data/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "data/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "data/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].path").value("data/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(5))
                .andExpect(jsonPath("$[0].type").value("FILE"));
    }

    @Test
    void testDirectoryMerge_conflictFile() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "data/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "data/")
                                .cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("File already exists, path=data/test.txt"));
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "data/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].path").value("data/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(5))
                .andExpect(jsonPath("$[0].type").value("FILE"));
    }

    @Test
    void testDirectoryMerge_twoFilesInOneQuery_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        MockMultipartFile file1 = new MockMultipartFile(
                "object",
                "cat.txt",
                MediaType.TEXT_PLAIN_VALUE,
                content
        );
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .file(file1)
                                .param("path", "data/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "data/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].path").value("data/"))
                .andExpect(jsonPath("$[0].name").value("cat.txt"))
                .andExpect(jsonPath("$[0].size").value(5))
                .andExpect(jsonPath("$[0].type").value("FILE"))
                .andExpect(jsonPath("$[1].path").value("data/"))
                .andExpect(jsonPath("$[1].name").value("test.txt"))
                .andExpect(jsonPath("$[1].size").value(5))
                .andExpect(jsonPath("$[1].type").value("FILE"));
    }

    @Test
    void testDirectoryMerge_nestedRelativeFilename_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        MockMultipartFile file = new MockMultipartFile(
                "object",
                "data/extra.txt",
                MediaType.TEXT_PLAIN_VALUE,
                content
        );
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "data/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "data/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].path").value("data/"))
                .andExpect(jsonPath("$[0].name").value("extra.txt"))
                .andExpect(jsonPath("$[0].size").value(5))
                .andExpect(jsonPath("$[0].type").value("FILE"));
    }
}
