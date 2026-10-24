package com.campus.market;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class UploadTest extends MarketTestSupport {
  @Test
  void validImageHasGeneratedSafeNameAndIsReadable() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB), "png", bytes);
    Browser b = login("student");
    String url =
        b.data(
                multipart("/api/uploads/image")
                    .file(
                        new MockMultipartFile(
                            "file", "../../照片.png", "image/png", bytes.toByteArray())))
            .path("url")
            .asText();
    assertTrue(url.matches("/uploads/[a-f0-9-]+\\.png"));
    Path path = Path.of("target/test-uploads", url.substring(9));
    try {
      assertArrayEquals(bytes.toByteArray(), Files.readAllBytes(path));
      mvc.perform(get(url))
          .andExpect(status().isOk())
          .andExpect(content().bytes(bytes.toByteArray()));
    } finally {
      Files.deleteIfExists(path);
    }
  }

  @Test
  void emptySpoofedWebpOversizedAndAnonymousUploadsAreRejected() throws Exception {
    Browser b = login("student");
    for (MockMultipartFile f :
        new MockMultipartFile[] {
          new MockMultipartFile("file", "a.png", "image/png", new byte[0]),
          new MockMultipartFile("file", "bad.jpg", "image/jpeg", "plain text".getBytes()),
          new MockMultipartFile("file", "bad.webp", "image/webp", "RIFF0000WEBPVP8 ".getBytes()),
          new MockMultipartFile("file", "big.png", "image/png", new byte[5 * 1024 * 1024 + 1])
        }) b.call(multipart("/api/uploads/image").file(f)).andExpect(status().isBadRequest());
    mvc.perform(
            multipart("/api/uploads/image")
                .file(new MockMultipartFile("file", "a.png", "image/png", new byte[] {1}))
                .header("X-User-Id", 1))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void pixelBombIsRejectedBeforeDecode() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", bytes);
    byte[] png = bytes.toByteArray();
    java.nio.ByteBuffer.wrap(png, 16, 8).putInt(10000).putInt(10000);
    login("student")
        .call(
            multipart("/api/uploads/image")
                .file(new MockMultipartFile("file", "bomb.png", "image/png", png)))
        .andExpect(status().isBadRequest());
  }
}
