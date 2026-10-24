package com.campus.market.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SessionAvailability extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    try {
      chain.doFilter(req, res);
    } catch (Exception e) {
      Throwable cause = e;
      boolean redis = false;
      while (cause != null) {
        if (cause instanceof org.springframework.data.redis.RedisConnectionFailureException
            || cause instanceof org.springframework.data.redis.RedisSystemException
            || cause instanceof io.lettuce.core.RedisException) redis = true;
        cause = cause.getCause();
      }
      if (redis && !res.isCommitted()) {
        res.reset();
        res.setStatus(503);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"code\":503,\"message\":\"登录服务暂不可用，请稍后重试\",\"data\":null}");
      } else if (e instanceof ServletException s) throw s;
      else if (e instanceof IOException i) throw i;
      else throw new ServletException(e);
    }
  }
}
