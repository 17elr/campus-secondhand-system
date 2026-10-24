package com.campus.market.auth;

import com.campus.market.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(0)
public class SessionAuth extends OncePerRequestFilter {
  private final UserMapper users;
  private final ObjectMapper json;

  public SessionAuth(UserMapper users, ObjectMapper json) {
    this.users = users;
    this.json = json;
  }

  public static String csrf(HttpSession s) {
    String token = (String) s.getAttribute("csrf");
    if (token == null) {
      token = UUID.randomUUID().toString();
      s.setAttribute("csrf", token);
    }
    return token;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    if (!req.getRequestURI().startsWith("/api/")) {
      chain.doFilter(req, res);
      return;
    }
    try {
      HttpSession session = req.getSession(false);
      User user = null;
      if (session != null && session.getAttribute("userId") instanceof Long id) {
        user = users.findById(id);
        if (user == null
            || user.getStatus() != 1
            || !Objects.equals(user.getAuthVersion(), session.getAttribute("authVersion"))) {
          session.invalidate();
          fail(res, 401, "登录已失效，请重新登录");
          return;
        }
      }
      String path = req.getRequestURI(), method = req.getMethod();
      boolean publicRead =
          method.equals("GET")
              && (path.equals("/api/goods")
                  || path.matches("/api/goods/\\d+")
                  || path.equals("/api/wants")
                  || path.matches("/api/messages/goods/\\d+")
                  || path.equals("/api/health"));
      boolean publicAuth =
          Set.of("/api/auth/csrf", "/api/auth/login", "/api/auth/register").contains(path);
      if (user == null && !publicRead && !publicAuth) {
        fail(res, 401, "请先登录");
        return;
      }
      if (path.startsWith("/api/admin/") && (user == null || !"ADMIN".equals(user.getRole()))) {
        fail(res, 403, "需要管理员权限");
        return;
      }
      if (!Set.of("GET", "HEAD", "OPTIONS").contains(method)) {
        String token = req.getHeader("X-CSRF-Token");
        if (session == null
            || token == null
            || session.getAttribute("csrf") == null
            || !MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                csrf(session).getBytes(StandardCharsets.UTF_8))) {
          fail(res, 403, "安全令牌失效，请刷新页面后重试");
          return;
        }
      }
      req.setAttribute("currentUser", user);
      chain.doFilter(req, res);
    } catch (DataAccessResourceFailureException | RedisSystemException e) {
      fail(res, 503, "登录服务暂不可用，请稍后重试");
    }
  }

  private void fail(HttpServletResponse r, int status, String message) throws IOException {
    r.setStatus(status);
    r.setContentType("application/json;charset=UTF-8");
    json.writeValue(r.getWriter(), new ApiResponse<>(status, message, null));
  }
}
