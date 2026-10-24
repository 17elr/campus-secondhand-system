package com.campus.market;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"demo", "test"})
abstract class MarketTestSupport {
  @Autowired protected MockMvc mvc;
  @Autowired protected ObjectMapper json;

  protected class Browser {
    Cookie cookie;
    String csrf;

    ResultActions call(MockHttpServletRequestBuilder req) throws Exception {
      if (cookie != null) req.cookie(cookie);
      if (csrf != null) req.header("X-CSRF-Token", csrf);
      ResultActions action = mvc.perform(req);
      Cookie next = action.andReturn().getResponse().getCookie("SESSION");
      if (next != null) cookie = next;
      return action;
    }

    JsonNode data(MockHttpServletRequestBuilder req) throws Exception {
      return json.readTree(
              call(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
          .path("data");
    }

    void token() throws Exception {
      csrf = data(get("/api/auth/csrf")).path("token").asText();
    }

    Browser login(String username) throws Exception {
      token();
      data(
          post("/api/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(
                  json.writeValueAsString(
                      java.util.Map.of(
                          "username",
                          username,
                          "password",
                          username.equals("admin") ? "admin123" : "123456"))));
      token();
      return this;
    }
  }

  protected Browser login(String username) throws Exception {
    return new Browser().login(username);
  }
}
