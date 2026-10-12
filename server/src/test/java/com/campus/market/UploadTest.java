package com.campus.market;

import com.campus.market.auth.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pagination-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class UploadTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private UserMapper users;

    @Test
    void uploadedImageIsStoredWithGeneratedNameAndCanBeViewed() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB), "png", bytes);
        String response = mvc
                .perform(multipart("/api/uploads/image")
                        .file(new MockMultipartFile("file", "../../照片.png", "image/png", bytes.toByteArray()))
                        .header("X-User-Id", 1))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String url = json.readTree(response).path("data").path("url").asText();
        assertTrue(url.matches("/uploads/[0-9a-f-]+\\.png"));
        Path stored = Path.of("uploads").resolve(url.substring("/uploads/".length())).toAbsolutePath()
                .normalize();
        try {
            assertArrayEquals(bytes.toByteArray(), Files.readAllBytes(stored));
            mvc.perform(get(url)).andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("image/png"))
                    .andExpect(content().bytes(bytes.toByteArray()));
        } finally {
            Files.deleteIfExists(stored);
        }
    }

    @Test
    void invalidEmptyOversizedAndDisabledUserUploadsAreRejected() throws Exception {
        mvc.perform(multipart("/api/uploads/image")
                .file(new MockMultipartFile("file", "bad.jpg", "image/jpeg", "plain text".getBytes()))
                .header("X-User-Id", 1)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/uploads/image")
                .file(new MockMultipartFile("file", "empty.png", "image/png", new byte[0]))
                .header("X-User-Id", 1)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/uploads/image")
                .file(new MockMultipartFile("file", "script.svg", "image/svg+xml", "<svg/>".getBytes()))
                .header("X-User-Id", 1)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/uploads/image")
                .file(new MockMultipartFile("file", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]))
                .header("X-User-Id", 1)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/uploads/image")
                .file(new MockMultipartFile("file", "x.png", "image/png", new byte[] { 1 }))
                .header("X-User-Id", users.findByUsername("gu").getId())).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/uploads/image")
                .file(new MockMultipartFile("file", "x.png", "image/png", new byte[] { 1 })))
                .andExpect(status().isBadRequest());
    }
}
