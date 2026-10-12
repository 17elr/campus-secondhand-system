package com.campus.market.upload;

import com.campus.market.common.ApiResponse;
import com.campus.market.auth.UserMapper;
import com.campus.market.auth.User;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final Path uploadRoot = Path.of("uploads").toAbsolutePath().normalize();
    private final UserMapper users;

    public UploadController(UserMapper users) {
        this.users = users;
    }

    @PostMapping("/image")
    public ApiResponse<Map<String, String>> image(@RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId) {
        User user = users.findById(userId);
        if (user == null || user.getStatus() != 1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号不可用");
        if (file == null || file.isEmpty())
            throw bad("请选择图片文件");
        if (file.getSize() > MAX_SIZE)
            throw bad("图片不能超过 5MB");
        String contentType = file.getContentType() == null ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!TYPES.contains(contentType))
            throw bad("仅支持 JPG、PNG、WEBP 图片");
        String extension = switch (contentType) {
        case "image/png" -> ".png";
        case "image/webp" -> ".webp";
        default -> ".jpg";
        };
        try {
            byte[] bytes = file.getBytes();
            boolean valid;
            if ("image/webp".equals(contentType)) {
                valid = bytes.length > 20
                        && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                        && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")
                        && Set.of("VP8 ", "VP8L", "VP8X").contains(
                                new String(bytes, 12, 4, java.nio.charset.StandardCharsets.US_ASCII));
            } else {
                boolean signature = "image/png".equals(contentType)
                        ? bytes.length > 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78
                                && bytes[3] == 71
                        : bytes.length > 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216
                                && (bytes[2] & 255) == 255;
                valid = signature && ImageIO.read(new ByteArrayInputStream(bytes)) != null;
            }
            if (!valid)
                throw bad("文件内容不是有效的图片");
            Files.createDirectories(uploadRoot);
            String filename = UUID.randomUUID() + extension;
            Path target = uploadRoot.resolve(filename).normalize();
            if (!target.startsWith(uploadRoot))
                throw bad("非法文件路径");
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            return ApiResponse.ok(Map.of("url", "/uploads/" + filename, "name", StringUtils
                    .cleanPath(file.getOriginalFilename() == null ? "商品图片" : file.getOriginalFilename())));
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败", error);
        }
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
