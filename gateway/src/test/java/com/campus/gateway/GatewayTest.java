package com.campus.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayTest {
  static HttpServer downstream;
  static AtomicReference<Map<String, String>> seen = new AtomicReference<>();

  @DynamicPropertySource
  static void url(DynamicPropertyRegistry registry) throws Exception {
    downstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    downstream.createContext(
        "/",
        exchange -> {
          Map<String, String> headers = new HashMap<>();
          for (String key :
              List.of("X-User-Id", "X-User-Role", "X-Internal-Token", "X-Request-Id", "Cookie"))
            headers.put(key, Objects.toString(exchange.getRequestHeaders().getFirst(key), ""));
          seen.set(headers);
          byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    downstream.start();
    registry.add("market.url", () -> "http://127.0.0.1:" + downstream.getAddress().getPort());
  }

  @Autowired MockMvc mvc;

  @AfterAll
  static void stop() {
    downstream.stop(0);
  }

  @Test
  void routesCookieButStripsForgedCredentialsAndGeneratesRequestId() throws Exception {
    String id =
        mvc.perform(
                get("/api/goods")
                    .header("Cookie", "SESSION=test-cookie")
                    .header("X-User-Id", "2")
                    .header("X-User-Role", "ADMIN")
                    .header("X-Internal-Token", "forged")
                    .header("X-Request-Id", "forged"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader("X-Request-Id");
    Map<String, String> h = seen.get();
    assertEquals("", h.get("X-User-Id"));
    assertEquals("", h.get("X-User-Role"));
    assertEquals("", h.get("X-Internal-Token"));
    assertEquals("SESSION=test-cookie", h.get("Cookie"));
    assertNotEquals("forged", id);
    assertEquals(id, h.get("X-Request-Id"));
  }

  @Test
  void internalRouteIsNotExposedAndLargeBodyIsRejected() throws Exception {
    mvc.perform(get("/internal/notifications/test")).andExpect(status().isNotFound());
    mvc.perform(post("/api/goods").content(new byte[6 * 1024 * 1024 + 1]))
        .andExpect(status().isPayloadTooLarge());
  }
}
