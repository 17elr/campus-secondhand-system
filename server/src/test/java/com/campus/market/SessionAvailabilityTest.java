package com.campus.market;

import static org.junit.jupiter.api.Assertions.*;

import com.campus.market.auth.SessionAvailability;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SessionAvailabilityTest {
  @Test
  void connectionFailureAndTimeoutBothReturn503() throws Exception {
    RuntimeException[] errors = {
      new org.springframework.data.redis.RedisConnectionFailureException("connection refused"),
      new QueryTimeoutException(
          "Redis command timed out", new io.lettuce.core.RedisCommandTimeoutException("timeout")),
      new org.springframework.data.redis.RedisSystemException(
          "session unavailable", new Exception())
    };
    for (RuntimeException error : errors) {
      var response = new MockHttpServletResponse();
      new SessionAvailability()
          .doFilter(
              new MockHttpServletRequest("GET", "/api/auth/me"),
              response,
              (request, result) -> {
                throw new ServletException(error);
              });
      assertEquals(503, response.getStatus());
      assertTrue(response.getContentAsString().contains("503"));
    }
  }

  @Test
  void unrelatedApplicationFailureIsNotHidden() {
    assertThrows(
        ServletException.class,
        () ->
            new SessionAvailability()
                .doFilter(
                    new MockHttpServletRequest("GET", "/api/auth/me"),
                    new MockHttpServletResponse(),
                    (request, result) -> {
                      throw new ServletException("application error");
                    }));
  }
}
