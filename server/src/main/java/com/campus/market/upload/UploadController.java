package com.campus.market.upload;

import static com.campus.market.common.Errors.*;

import com.campus.market.common.ApiResponse;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
  private final Path root;

  public UploadController(@Value("${market.upload-dir}") String root) {
    this.root = Path.of(root).toAbsolutePath().normalize();
  }

  @PostMapping("/image")
  public ApiResponse<?> image(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) throw bad("图片为空或超过5MB");
    String mime = Objects.toString(file.getContentType(), "").toLowerCase(Locale.ROOT);
    String format =
        switch (mime) {
          case "image/png" -> "png";
          case "image/jpeg" -> "JPEG";
          default -> throw bad("请上传JPG/PNG，WebP请先转换");
        };
    try (ImageInputStream input =
        ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) throw bad("文件内容不是有效图片");
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        int w = reader.getWidth(0), h = reader.getHeight(0);
        if (w < 1 || h < 1 || w > 10000 || h > 10000 || (long) w * h > 20000000)
          throw bad("图片尺寸超过限制（2000万像素）");
        if (!reader.getFormatName().equalsIgnoreCase(format) || reader.read(0) == null)
          throw bad("图片格式或内容无效");
      } finally {
        reader.dispose();
      }
      Files.createDirectories(root);
      String filename = UUID.randomUUID() + (format.equals("png") ? ".png" : ".jpg");
      Path target = root.resolve(filename).normalize();
      if (!target.startsWith(root)) throw bad("非法文件路径");
      Files.write(target, file.getBytes(), StandardOpenOption.CREATE_NEW);
      return ApiResponse.ok(Map.of("url", "/uploads/" + filename));
    } catch (javax.imageio.IIOException e) {
      throw bad("图片内容损坏");
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败", e);
    }
  }
}
