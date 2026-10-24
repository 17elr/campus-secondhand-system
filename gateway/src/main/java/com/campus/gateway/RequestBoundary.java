package com.campus.gateway;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestBoundary extends OncePerRequestFilter {
  private static final long MAX = 6 * 1024 * 1024;

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    String id = UUID.randomUUID().toString();
    res.setHeader("X-Request-Id", id);
    res.setHeader("X-Content-Type-Options", "nosniff");
    if (req.getContentLengthLong() > MAX) {
      res.setStatus(413);
      return;
    }
    HttpServletRequestWrapper bounded =
        new HttpServletRequestWrapper(req) {
          @Override
          public String getHeader(String name) {
            return name.equalsIgnoreCase("X-Request-Id") ? id : super.getHeader(name);
          }

          @Override
          public java.util.Enumeration<String> getHeaders(String name) {
            return name.equalsIgnoreCase("X-Request-Id")
                ? java.util.Collections.enumeration(java.util.List.of(id))
                : super.getHeaders(name);
          }

          @Override
          public java.util.Enumeration<String> getHeaderNames() {
            java.util.Set<String> names =
                new java.util.LinkedHashSet<>(java.util.Collections.list(super.getHeaderNames()));
            names.removeIf(n -> n.equalsIgnoreCase("X-Request-Id"));
            names.add("X-Request-Id");
            return java.util.Collections.enumeration(names);
          }

          @Override
          public ServletInputStream getInputStream() throws IOException {
            ServletInputStream input = super.getInputStream();
            return new ServletInputStream() {
              long count;

              private void limit(int size) throws IOException {
                if (size > 0 && (count += size) > MAX) throw new LimitException();
              }

              @Override
              public int read() throws IOException {
                int b = input.read();
                limit(b < 0 ? 0 : 1);
                return b;
              }

              @Override
              public int read(byte[] b, int off, int len) throws IOException {
                int size = input.read(b, off, len);
                limit(size);
                return size;
              }

              @Override
              public boolean isFinished() {
                return input.isFinished();
              }

              @Override
              public boolean isReady() {
                return input.isReady();
              }

              @Override
              public void setReadListener(ReadListener listener) {
                input.setReadListener(listener);
              }
            };
          }
        };
    try {
      chain.doFilter(bounded, res);
    } catch (Exception e) {
      Throwable root = e;
      while (root.getCause() != null) root = root.getCause();
      if (root instanceof LimitException && !res.isCommitted()) {
        res.resetBuffer();
        res.setStatus(413);
      } else if (e instanceof ServletException se) throw se;
      else if (e instanceof IOException io) throw io;
      else throw new ServletException(e);
    }
  }

  private static class LimitException extends IOException {}
}
