package com.campus.market.auth;

import com.campus.market.common.ApiResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserMapper mapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserMapper mapper) {
        this.mapper = mapper;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username"), password = body.get("password");
        if (username == null || username.isBlank() || password == null || password.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写用户名和密码");
        User user = mapper.findByUsername(username.trim());
        if (user == null || user.getStatus() != 1 || !encoder.matches(password, user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误，或账号已被禁用");
        return ApiResponse.ok(AuthService.view(user));
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");
        String nickname = body.getOrDefault("nickname", username).trim();
        if (username.length() < 3 || password.length() < 6)
            throw new IllegalArgumentException("用户名至少 3 位，密码至少 6 位");
        if (mapper.findByUsername(username) != null)
            throw new IllegalArgumentException("用户名已存在");
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setNickname(nickname.isBlank() ? username : nickname);
        user.setRole("USER");
        mapper.insert(user);
        return ApiResponse.ok(AuthService.view(mapper.findByUsername(username)));
    }
}
