package com.campus.market.auth;

import static com.campus.market.common.Errors.*;

import com.campus.market.common.ApiResponse;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final UserMapper users;
  private final BCryptPasswordEncoder encoder;

  public AuthController(UserMapper users, BCryptPasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

  @GetMapping("/csrf")
  public ApiResponse<?> csrf(HttpServletRequest req) {
    return ApiResponse.ok(Map.of("token", SessionAuth.csrf(req.getSession())));
  }

  @GetMapping("/me")
  public ApiResponse<?> me(@RequestAttribute("currentUser") User user) {
    return ApiResponse.ok(AuthService.view(user));
  }

  @PostMapping("/login")
  public ApiResponse<?> login(@RequestBody Map<String, String> body, HttpServletRequest req) {
    String name = body.get("username"), password = body.get("password");
    if (name == null
        || name.isBlank()
        || name.length() > 40
        || password == null
        || password.isEmpty()
        || password.length() > 72) throw bad("请填写有效用户名和密码");
    User user = users.findByUsername(name.trim());
    if (user == null || user.getStatus() != 1 || !encoder.matches(password, user.getPasswordHash()))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误，或账号已被禁用");
    return signedIn(user, req);
  }

  @PostMapping("/register")
  public ApiResponse<?> register(@RequestBody Map<String, String> body, HttpServletRequest req) {
    String name = Objects.toString(body.get("username"), "").trim(),
        password = Objects.toString(body.get("password"), ""),
        nickname = Objects.toString(body.get("nickname"), name).trim();
    if (!name.matches("[A-Za-z0-9_]{3,40}")
        || password.length() < 6
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
        || nickname.isEmpty()
        || nickname.length() > 40) throw bad("用户名应为3至40位字母数字下划线，密码至少6位，昵称1至40字");
    if (users.findByUsername(name) != null) throw conflict("用户名已存在");
    User user = new User();
    user.setUsername(name);
    user.setNickname(nickname);
    user.setPasswordHash(encoder.encode(password));
    user.setRole("USER");
    users.insert(user);
    return signedIn(users.findById(user.getId()), req);
  }

  private ApiResponse<?> signedIn(User user, HttpServletRequest req) {
    HttpSession s = req.getSession();
    req.changeSessionId();
    s.setAttribute("userId", user.getId());
    s.setAttribute("authVersion", user.getAuthVersion());
    s.removeAttribute("csrf");
    SessionAuth.csrf(s);
    return ApiResponse.ok(AuthService.view(user));
  }

  @PostMapping("/logout")
  public ApiResponse<?> logout(HttpServletRequest req) {
    req.getSession().invalidate();
    return ApiResponse.ok(true);
  }

  @PutMapping("/profile")
  public ApiResponse<?> profile(
      @RequestAttribute("currentUser") User user, @RequestBody Map<String, String> body) {
    String name = Objects.toString(body.get("nickname"), "").trim();
    if (name.isEmpty() || name.length() > 40) throw bad("昵称应为1至40字");
    users.profile(user.getId(), name);
    return ApiResponse.ok(AuthService.view(users.findById(user.getId())));
  }

  @PutMapping("/password")
  public ApiResponse<?> password(
      @RequestAttribute("currentUser") User user,
      @RequestBody Map<String, String> body,
      HttpServletRequest req) {
    String old = Objects.toString(body.get("oldPassword"), ""),
        next = Objects.toString(body.get("newPassword"), "");
    if (!encoder.matches(old, user.getPasswordHash())) throw bad("原密码不正确");
    if (next.length() < 6 || next.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw bad("新密码至少6位且不超过72字节");
    if (users.password(user.getId(), encoder.encode(next), user.getPasswordHash()) != 1)
      throw conflict("密码已变化，请重新登录");
    req.getSession().invalidate();
    return ApiResponse.ok(true);
  }
}
