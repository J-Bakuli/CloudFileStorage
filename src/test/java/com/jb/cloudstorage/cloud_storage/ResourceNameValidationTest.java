package com.jb.cloudstorage.cloud_storage;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResourceNameValidationTest extends BaseApiIntegrationTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "../secret.txt",
            "folder/../secret.txt",
            "folder\\x.txt",
            "a//b.txt",
            "/abs.txt",
            "file:name.txt",
            "file*name.txt",
            "file?name.txt",
            "file\"name.txt",
            "file<name.txt",
            "file>name.txt",
            "file|name.txt"
    })
    void testUploadResource_invalid_fileName(String fileName) throws Exception {
        basicSignUp();
        MockMultipartFile file = new MockMultipartFile(
                "object",
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes()
        );
        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "")
                                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid filename"));
    }
}
