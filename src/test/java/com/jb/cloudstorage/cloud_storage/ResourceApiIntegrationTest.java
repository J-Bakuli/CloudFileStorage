package com.jb.cloudstorage.cloud_storage;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized request"))
                .andExpect(jsonPath("$.path").value("/api/resource"));
    }

    @Test
    void testUpload_emptyFile() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        byte[] content = "".getBytes();
        MockMultipartFile emptyFile = new MockMultipartFile(
                "object",
                "text.txt",
                MediaType.TEXT_PLAIN_VALUE,
                content
        );
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(emptyFile)
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("File is null or empty"));
    }

    @Test
    void testUpload_parentPathMustEndWithSlash() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "exam")
                                .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Directory path must end with /")));
    }

    @Test
    void testUpload_nullFilename() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        MockMultipartFile fileWithoutName = new MockMultipartFile(
                "object",
                null,
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes()
        );
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(fileWithoutName)
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("File name is missing"));
    }

    @Test
    void testUpload_nestedRelativePath_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        MockMultipartFile nestedFile = new MockMultipartFile(
                "object",
                "upload_folder/test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                content
        );
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(nestedFile)
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value("upload_folder/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(content.length))
                .andExpect(jsonPath("$[0].type").value("FILE"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "upload_folder/test.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("upload_folder/"))
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @Test
    void testUpload_nestedRelativePath_withTargetFolder_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        MockMultipartFile nestedFile = new MockMultipartFile(
                "object",
                "upload_folder/test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                content
        );
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(nestedFile)
                                .param("path", "storage_folder/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value("storage_folder/upload_folder/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(content.length))
                .andExpect(jsonPath("$[0].type").value("FILE"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "storage_folder/upload_folder/test.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("storage_folder/upload_folder/"))
                .andExpect(jsonPath("$.name").value("test.txt"));
    }

    @Test
    void testUpload_multipleFiles_returnsAll() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        MockMultipartFile file1 = new MockMultipartFile(
                "object",
                "one.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "one".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "object",
                "two.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "two".getBytes()
        );
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file1)
                                .file(file2)
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("one.txt"))
                .andExpect(jsonPath("$[1].name").value("two.txt"));
    }

    @Test
    void testGet_unauthenticated() throws Exception {
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "test.txt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized request"))
                .andExpect(jsonPath("$.path").value("/api/resource"));
    }

    @Test
    void testGet_notFound() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "noFile.txt")
                                .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void testGet_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        getBasicFile(cookie);
    }

    @Test
    void testUploadFile_conflict() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testUploadFile_conflict_caseInsensitiveName() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        MockMultipartFile upperCaseNameFile = new MockMultipartFile(
                "object",
                "TEST.TXT",
                MediaType.TEXT_PLAIN_VALUE,
                content
        );
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "exam/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(upperCaseNameFile)
                                .param("path", "exam/")
                                .cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testUploadFile_conflict_caseInsensitiveName_sameRequest() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        MockMultipartFile file1 = new MockMultipartFile(
                "object",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "object",
                "TEST.TXT",
                MediaType.TEXT_PLAIN_VALUE,
                "world".getBytes()
        );
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file1)
                                .file(file2)
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "test.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "TEST.TXT")
                                .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void testDeleteFile_unauthorized() throws Exception {
        mockMvc.perform(
                        delete("/api/resource")
                                .param("path", "test.txt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized request"))
                .andExpect(jsonPath("$.path").value("/api/resource"));
    }

    @Test
    void testDeleteFile_notFound() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        delete("/api/resource")
                                .param("path", "noFile.txt")
                                .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testDeleteFile_Success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        delete("/api/resource")
                                .param("path", "exam/test.txt")
                                .cookie(cookie))
                .andExpect(status().isNoContent());
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteDirectory_Success() throws Exception {
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
                        delete("/api/resource")
                                .param("path", "newdir/")
                                .cookie(cookie))
                .andExpect(status().isNoContent());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "newdir/")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteDirectory_createdViaFileUpload_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "exam/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].path").value("exam/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(5))
                .andExpect(jsonPath("$[0].type").value("FILE"));
        mockMvc.perform(
                        delete("/api/resource")
                                .param("path", "exam/")
                                .cookie(cookie))
                .andExpect(status().isNoContent());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "exam/")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDownloadFile_unauthorized() throws Exception {
        mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", "test.txt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized request"))
                .andExpect(jsonPath("$.path").value("/api/resource/download"));
    }

    @Test
    void testDownloadFile_notFound() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", "noFile.txt")
                                .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testDownloadFile_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        get("/api/resource/download")
                                .param("path", "exam/test.txt")
                                .cookie(cookie))
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized request"))
                .andExpect(jsonPath("$.path").value("/api/resource/move"));
    }

    @Test
    void testMoveFile_fromResourceNotFound() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "test/test.txt")
                                .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("is not found")));
    }

    @Test
    void testMoveFile_parentToDoesNotExist() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "test/test.txt")
                                .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Parent directory is not found")));
    }

    @Test
    void testMoveFile_conflict() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "test/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "test/test.txt")
                                .param("to", "exam/test.txt")
                                .cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testMoveFile_resource_type_change() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "exam/test.txt/")
                                .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid operation request")));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @Test
    void testMoveFile_resource_type_change_another_parent() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam1/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "exam1/test.txt/")
                                .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid operation request")));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @Test
    void testMoveFile_conflict_caseInsensitiveName() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "test/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "test/test.txt")
                                .param("to", "exam/TEST.TXT")
                                .cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testMoveFile_sameNameDifferentParent_caseInsensitive_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "daily/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "daily/test.txt")
                                .param("to", "projects/TEST.TXT")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("projects/"))
                .andExpect(jsonPath("$.name").value("TEST.TXT"))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @Test
    void testMoveFile_sameNameAsDirectory_differentType_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "TEST.TXT/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "other/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "other/test.txt")
                                .param("to", "test.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @Test
    void testMoveFile_move_success() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam1/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "exam1/test.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(content.length))
                .andExpect(jsonPath("$.path").value("exam1/"))
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam1/test.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"));
    }

    @Test
    void testMoveFile_rename_caseInsensitiveRename() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "exam/tESt.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(content.length))
                .andExpect(jsonPath("$.path").value("exam/"))
                .andExpect(jsonPath("$.name").value("tESt.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/tESt.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("tESt.txt"));
    }

    @Test
    void testCoexistence_file_and_directory_areAccessible() throws Exception {
        Cookie cookie = prepareCoexistenceState();
        assertDirectoryContains(cookie, "exam/");
        assertStandaloneFileExists(cookie, "exam");
    }

    @Test
    void testCoexistence_file_deletion_directory_isAccessible() throws Exception {
        Cookie cookie = prepareCoexistenceState();
        mockMvc.perform(
                        delete("/api/resource")
                                .param("path", "exam")
                                .cookie(cookie))
                .andExpect(status().isNoContent());
        assertDirectoryContains(cookie, "exam/");
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam")
                                .cookie(cookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCoexistence_directory_deletion_file_isAccessible() throws Exception {
        Cookie cookie = prepareCoexistenceState();
        mockMvc.perform(
                        delete("/api/resource")
                                .param("path", "exam/")
                                .cookie(cookie))
                .andExpect(status().isNoContent());
        assertDirectoryNotFound(cookie, "exam/");
        assertStandaloneFileExists(cookie, "exam");
    }

    @Test
    void testCoexistence_file_rename_and_back_file_and_directory_areAccessible() throws Exception {
        Cookie cookie = prepareCoexistenceState();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam")
                                .param("to", "exam1")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam1"))
                .andExpect(jsonPath("$.type").value("FILE"));
        assertStandaloneFileExists(cookie, "exam1");
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam1")
                                .param("to", "exam")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam"))
                .andExpect(jsonPath("$.type").value("FILE"));
        assertDirectoryContains(cookie, "exam/");
        assertStandaloneFileExists(cookie, "exam");
    }

    @Test
    void testCoexistence_directory_move_and_back_file_and_directory_areAccessible() throws Exception {
        Cookie cookie = prepareCoexistenceState();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/")
                                .param("to", "exam1/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam1"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        assertDirectoryNotFound(cookie, "exam/");
        assertDirectoryContains(cookie, "exam1/");
        assertStandaloneFileExists(cookie, "exam");
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam1/")
                                .param("to", "exam/")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        assertDirectoryContains(cookie, "exam/");
        assertDirectoryNotFound(cookie, "exam1/");
        assertStandaloneFileExists(cookie, "exam");
    }

    @Test
    void testSearch_unauthenticated() throws Exception {
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "timelines"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized request"))
                .andExpect(jsonPath("$.path").value("/api/resource/search"));
    }

    @Test
    void testSearch_notFound() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "timelines")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(0)))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void testSearch_blankQuery() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "")
                                .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid request")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("Query is empty")));
    }

    @Test
    void testSearch_success_searchFile_trimAndPartialName() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "test.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].path", hasItem("exam/")))
                .andExpect(jsonPath("$[*].name", hasItem("test.txt")))
                .andExpect(jsonPath("$[*].size", hasItem(content.length)))
                .andExpect(jsonPath("$[*].type", hasItem("FILE")));
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "tes")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].path", hasItem("exam/")))
                .andExpect(jsonPath("$[*].name", hasItem("test.txt")))
                .andExpect(jsonPath("$[*].size", hasItem(content.length)))
                .andExpect(jsonPath("$[*].type", hasItem("FILE")));
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "EXam")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].path", hasItem("exam/")))
                .andExpect(jsonPath("$[*].name", hasItem("test.txt")))
                .andExpect(jsonPath("$[*].size", hasItem(content.length)))
                .andExpect(jsonPath("$[*].type", hasItem("FILE")));
    }

    @Test
    void testSearch_success_searchFile_with_same_fileName_in_several_folders() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "daily/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "test.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].path", hasItems("daily/", "exam/")))
                .andExpect(jsonPath("$[*].name", hasItem("test.txt")))
                .andExpect(jsonPath("$[*].size", hasItem(content.length)))
                .andExpect(jsonPath("$[*].type", hasItem("FILE")));
    }

    @Test
    void testSearch_success_searchFile_in_nested_directories() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "level1/level2/level3/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "test.txt")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].path", hasItem("level1/level2/level3/")))
                .andExpect(jsonPath("$[*].name", hasItem("test.txt")))
                .andExpect(jsonPath("$[*].size", hasItem(content.length)))
                .andExpect(jsonPath("$[*].type", hasItem("FILE")));
    }

    @Test
    void testSearch_success_searchDirectory_without_file() throws Exception {
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
                        get("/api/resource/search")
                                .param("query", "level1")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].size").doesNotExist())
                .andExpect(jsonPath("$[*].type", hasItem("DIRECTORY")));
    }

    @Test
    void testSearch_success_searchDirectory_with_one_directory_per_time_creation_without_file() throws Exception {
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
                        get("/api/resource/search")
                                .param("query", "level1/level2")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].path", hasItems("level1/", "level1/level2/")))
                .andExpect(jsonPath("$[*].name", hasItems("level2", "level3")))
                .andExpect(jsonPath("$[*].size").doesNotExist())
                .andExpect(jsonPath("$[*].type", hasItem("DIRECTORY")));
    }

    @Test
    void testSearch_success_explicit_dir_creation_with_file() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam/")
                                .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam"))
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        uploadBasicFile(cookie);
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "exam")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].path", hasItems("exam/", "")))
                .andExpect(jsonPath("$[*].name", hasItems("test.txt", "exam")))
                .andExpect(jsonPath("$[*].type", hasItems("FILE", "DIRECTORY")));
    }

    @Test
    void testSearch_success_without_explicit_dir_creation_with_file() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        uploadBasicFile(cookie);
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "exam")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].path", hasItem("exam/")))
                .andExpect(jsonPath("$[*].name", hasItem("test.txt")))
                .andExpect(jsonPath("$[*].size", hasItem(content.length)))
                .andExpect(jsonPath("$[*].type", hasItem("FILE")));
    }

    private Cookie prepareCoexistenceState() throws Exception {
        Cookie cookie = basicSignUpAndSessionCookie();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "exam/")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        MockMultipartFile sourceFile = new MockMultipartFile(
                "object",
                "exam.txt",
                MediaType.TEXT_PLAIN_VALUE,
                content
        );
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(sourceFile)
                                .param("path", "")
                                .cookie(cookie))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam.txt")
                                .param("to", "exam")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam"))
                .andExpect(jsonPath("$.type").value("FILE"));
        return cookie;
    }

    private void assertStandaloneFileExists(Cookie cookie, String path) throws Exception {
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", path)
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value(path))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    private void assertDirectoryNotFound(Cookie cookie, String path) throws Exception {
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", path)
                                .cookie(cookie))
                .andExpect(status().isNotFound());
    }

    private void assertDirectoryContains(Cookie cookie, String path) throws Exception {
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", path)
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].size").value(5))
                .andExpect(jsonPath("$[0].path").value(path))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].type").value("FILE"));
    }
}
