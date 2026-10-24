package com.campus.notice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=${TEST_NOTICE_DB_URL:jdbc:mysql://127.0.0.1:13319/notice_test?serverTimezone=Asia/Shanghai}",
      "spring.datasource.username=${TEST_NOTICE_DB_USER:notice}",
      "spring.datasource.password=${TEST_NOTICE_DB_PASSWORD:local-notice-test}",
      "notice.internal-token=campus-test-internal-token-at-least-32-characters"
    })
@AutoConfigureMockMvc
class NoticeTest {
  @Autowired MockMvc mvc;
  String token = "campus-test-internal-token-at-least-32-characters";

  @Test
  void internalCredentialIsRequired() throws Exception {
    mvc.perform(get("/internal/users/1/notifications")).andExpect(status().isForbidden());
  }

  @Test
  void repeatedEventIsIdempotentAndDifferentPayloadConflicts() throws Exception {
    String id = UUID.randomUUID().toString(),
        payload =
            "{\"recipientId\":1,\"type\":\"ORDER_CREATED\",\"businessId\":1,\"title\":\"新意向\",\"body\":\"图书馆面交\",\"occurredAt\":\"2026-10-24T00:00:00Z\"}";
    for (int i = 0; i < 2; i++)
      mvc.perform(
              put("/internal/notifications/" + id)
                  .header("X-Internal-Token", token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(payload))
          .andExpect(status().isOk());
    mvc.perform(
            put("/internal/notifications/" + id)
                .header("X-Internal-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload.replace("图书馆面交", "其他内容")))
        .andExpect(status().isConflict());
  }

  @Test
  void readRequiresRecipientOwnershipAndIsIdempotent() throws Exception {
    String payload =
        "{\"recipientId\":5,\"type\":\"TEST\",\"businessId\":1,\"title\":\"测试\",\"body\":\"内容\",\"occurredAt\":\"2026-10-24T00:00:00Z\"}";
    String result =
        mvc.perform(
                put("/internal/notifications/" + UUID.randomUUID())
                    .header("X-Internal-Token", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(result).path("id").asLong();
    mvc.perform(
            put("/internal/users/6/notifications/" + id + "/read")
                .header("X-Internal-Token", token))
        .andExpect(status().isNotFound());
    for (int i = 0; i < 2; i++)
      mvc.perform(
              put("/internal/users/5/notifications/" + id + "/read")
                  .header("X-Internal-Token", token))
          .andExpect(status().isOk());
  }
}
