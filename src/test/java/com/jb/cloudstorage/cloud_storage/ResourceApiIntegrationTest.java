package com.jb.cloudstorage.cloud_storage;

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
        basicSignUp();
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
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("File is null or empty"));
    }

    @Test
    void testUpload_nullFilename() throws Exception {
        basicSignUp();
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
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("File name is missing"));
    }

    @Test
    void testUpload_nestedRelativePath_success() throws Exception {
        basicSignUp();
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
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value("upload_folder/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(content.length))
                .andExpect(jsonPath("$[0].type").value("FILE"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "upload_folder/test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("upload_folder/"))
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @Test
    void testUpload_nestedRelativePath_withTargetFolder_success() throws Exception {
        basicSignUp();
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
                                .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].path").value("storage_folder/upload_folder/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(content.length))
                .andExpect(jsonPath("$[0].type").value("FILE"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "storage_folder/upload_folder/test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("storage_folder/upload_folder/"))
                .andExpect(jsonPath("$.name").value("test.txt"));
    }

    @Test
    void testUpload_multipleFiles_returnsAll() throws Exception {
        basicSignUp();
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
                                .session(session))
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
                                .param("path", "exam/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(upperCaseNameFile)
                                .param("path", "exam/")
                                .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void testUploadFile_conflict_caseInsensitiveName_sameRequest() throws Exception {
        basicSignUp();
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
                                .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "TEST.TXT")
                                .session(session))
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
    void testDeleteDirectory_createdViaFileUpload_success() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "exam/")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].path").value("exam/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(5))
                .andExpect(jsonPath("$[0].type").value("FILE"));
        mockMvc.perform(
                        delete("/api/resource")
                                .param("path", "exam/")
                                .session(session))
                .andExpect(status().isNoContent());
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", "exam/")
                                .session(session))
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized request"))
                .andExpect(jsonPath("$.path").value("/api/resource/move"));
    }

    @Test
    void testMoveFile_fromResourceNotFound() throws Exception {
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
    void testMoveFile_parentToDoesNotExist() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "test/test.txt")
                                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Parent directory is not found")));
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
    void testMoveFile_resource_type_change() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "exam/test.txt/")
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid operation request")));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @Test
    void testMoveFile_resource_type_change_another_parent() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam1/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "exam1/test.txt/")
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid operation request")));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @Test
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
    void testMoveFile_sameNameDifferentParent_caseInsensitive_success() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "daily")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "projects/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "daily/test.txt")
                                .param("to", "projects/TEST.TXT")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("projects/"))
                .andExpect(jsonPath("$.name").value("TEST.TXT"))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @Test
    void testMoveFile_sameNameAsDirectory_differentType_success() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "TEST.TXT/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "other")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "other/test.txt")
                                .param("to", "test.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    @Test
    void testMoveFile_move_success() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam1/")
                                .session(session))
                .andExpect(status().isCreated());
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
    void testMoveFile_rename_caseInsensitiveRename() throws Exception {
        basicSignUp();
        uploadBasicFile();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/test.txt")
                                .param("to", "exam/tESt.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(content.length))
                .andExpect(jsonPath("$.path").value("exam/"))
                .andExpect(jsonPath("$.name").value("tESt.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/test.txt")
                                .session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam/tESt.txt")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("tESt.txt"));
    }

    @Test
    void testCoexistence_file_and_directory_areAccessible() throws Exception {
        prepareCoexistenceState();
        assertDirectoryContains("exam/");
        assertStandaloneFileExists("exam");
    }

    @Test
    void testCoexistence_file_deletion_directory_isAccessible() throws Exception {
        prepareCoexistenceState();
        mockMvc.perform(
                        delete("/api/resource")
                                .param("path", "exam")
                                .session(session))
                .andExpect(status().isNoContent());
        assertDirectoryContains("exam/");
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", "exam")
                                .session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCoexistence_directory_deletion_file_isAccessible() throws Exception {
        prepareCoexistenceState();
        mockMvc.perform(
                        delete("/api/resource")
                                .param("path", "exam/")
                                .session(session))
                .andExpect(status().isNoContent());
        assertDirectoryNotFound("exam/");
        assertStandaloneFileExists("exam");
    }

    @Test
    void testCoexistence_file_rename_and_back_file_and_directory_areAccessible() throws Exception {
        prepareCoexistenceState();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam")
                                .param("to", "exam1")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam1"))
                .andExpect(jsonPath("$.type").value("FILE"));
        assertStandaloneFileExists("exam1");
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam1")
                                .param("to", "exam")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam"))
                .andExpect(jsonPath("$.type").value("FILE"));
        assertDirectoryContains("exam/");
        assertStandaloneFileExists("exam");
    }

    @Test
    void testCoexistence_directory_move_and_back_file_and_directory_areAccessible() throws Exception {
        prepareCoexistenceState();
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam/")
                                .param("to", "exam1/")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam1"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        assertDirectoryNotFound("exam/");
        assertDirectoryContains("exam1/");
        assertStandaloneFileExists("exam");
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam1/")
                                .param("to", "exam/")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").doesNotExist())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        assertDirectoryContains("exam/");
        assertDirectoryNotFound("exam1/");
        assertStandaloneFileExists("exam");
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
                .andExpect(jsonPath("$.message", containsString("Invalid request")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("Query is empty")));
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
                .andExpect(jsonPath("$[*].path", hasItem("exam/")))
                .andExpect(jsonPath("$[*].name", hasItem("test.txt")))
                .andExpect(jsonPath("$[*].size", hasItem(content.length)))
                .andExpect(jsonPath("$[*].type", hasItem("FILE")));
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "tes")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].path", hasItem("exam/")))
                .andExpect(jsonPath("$[*].name", hasItem("test.txt")))
                .andExpect(jsonPath("$[*].size", hasItem(content.length)))
                .andExpect(jsonPath("$[*].type", hasItem("FILE")));
        mockMvc.perform(
                        get("/api/resource/search")
                                .param("query", "EXam")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].path", hasItem("exam/")))
                .andExpect(jsonPath("$[*].name", hasItem("test.txt")))
                .andExpect(jsonPath("$[*].size", hasItem(content.length)))
                .andExpect(jsonPath("$[*].type", hasItem("FILE")));
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
                .andExpect(jsonPath("$[*].path", hasItems("daily/", "exam/")))
                .andExpect(jsonPath("$[*].name", hasItem("test.txt")))
                .andExpect(jsonPath("$[*].size", hasItem(content.length)))
                .andExpect(jsonPath("$[*].type", hasItem("FILE")));
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
                .andExpect(jsonPath("$[*].path", hasItem("level1/level2/level3/")))
                .andExpect(jsonPath("$[*].name", hasItem("test.txt")))
                .andExpect(jsonPath("$[*].size", hasItem(content.length)))
                .andExpect(jsonPath("$[*].type", hasItem("FILE")));
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
                .andExpect(jsonPath("$[*].size").doesNotExist())
                .andExpect(jsonPath("$[*].type", hasItem("DIRECTORY")));
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
                .andExpect(jsonPath("$[*].path", hasItems("level1/", "level1/level2/")))
                .andExpect(jsonPath("$[*].name", hasItems("level2", "level3")))
                .andExpect(jsonPath("$[*].size").doesNotExist())
                .andExpect(jsonPath("$[*].type", hasItem("DIRECTORY")));
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
                .andExpect(jsonPath("$[*].path", hasItems("exam/", "")))
                .andExpect(jsonPath("$[*].name", hasItems("test.txt", "exam")))
                .andExpect(jsonPath("$[*].type", hasItems("FILE", "DIRECTORY")));
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
                .andExpect(jsonPath("$[*].path", hasItem("exam/")))
                .andExpect(jsonPath("$[*].name", hasItem("test.txt")))
                .andExpect(jsonPath("$[*].size", hasItem(content.length)))
                .andExpect(jsonPath("$[*].type", hasItem("FILE")));
    }

    private void prepareCoexistenceState() throws Exception {
        basicSignUp();
        mockMvc.perform(
                        post("/api/directory")
                                .param("path", "exam/")
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "exam/")
                                .session(session))
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
                                .session(session))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/resource/move")
                                .param("from", "exam.txt")
                                .param("to", "exam")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value("exam"))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    private void assertStandaloneFileExists(String path) throws Exception {
        mockMvc.perform(
                        get("/api/resource")
                                .param("path", path)
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(""))
                .andExpect(jsonPath("$.name").value(path))
                .andExpect(jsonPath("$.type").value("FILE"));
    }

    private void assertDirectoryNotFound(String path) throws Exception {
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", path)
                                .session(session))
                .andExpect(status().isNotFound());
    }

    private void assertDirectoryContains(String path) throws Exception {
        mockMvc.perform(
                        get("/api/directory")
                                .param("path", path)
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].size").value(5))
                .andExpect(jsonPath("$[0].path").value(path))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].type").value("FILE"));
    }
}
