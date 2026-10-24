package com.campus.notice;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalAuth extends OncePerRequestFilter {
  private final String token;

  public InternalAuth(@Value("${notice.internal-token}") String token) {
    if (token.length() < 32)
      throw new IllegalArgumentException(
          "NOTICE_INTERNAL_TOKEN must contain at least 32 characters");
    this.token = token;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    String supplied = req.getHeader("X-Internal-Token");
    if (supplied == null
        || !MessageDigest.isEqual(
            token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
      res.setStatus(403);
      return;
    }
    chain.doFilter(req, res);
  }
}
