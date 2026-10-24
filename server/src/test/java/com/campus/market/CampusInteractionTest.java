package com.campus.market;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CampusInteractionTest extends MarketTestSupport {
  @Autowired JdbcTemplate jdbc;

  @Test
  void ownerEditsWantAndTerminalStateCannotBeReopened() throws Exception {
    Browser owner = login("student"), other = login("zhou"), admin = login("admin");
    String payload =
        "{\"title\":\"测试求购\",\"category\":\"教材书籍\",\"budget\":25,\"description\":\"图书馆面交\"}";
    long id =
        owner
            .data(post("/api/wants").contentType(MediaType.APPLICATION_JSON).content(payload))
            .path("id")
            .asLong();
    other
        .call(put("/api/wants/" + id).contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isForbidden());
    owner
        .call(put("/api/wants/" + id).contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isOk());
    admin
        .call(
            patch("/api/admin/wants/" + id + "/status")
                .param("value", "PUBLISHED")
                .param("reason", "审核通过"))
        .andExpect(status().isOk());
    owner
        .call(patch("/api/wants/" + id + "/status").param("value", "FOUND"))
        .andExpect(status().isOk());
    admin
        .call(
            patch("/api/admin/wants/" + id + "/status")
                .param("value", "PUBLISHED")
                .param("reason", "再次审核"))
        .andExpect(status().isConflict());
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from outbox_event where type='WANT_REVIEWED' and business_id=?",
            Integer.class,
            id));
  }

  @Test
  void sellerReplyUsesIdDespiteIdenticalNicknames() throws Exception {
    Browser seller = login("student"), buyer = login("zhou"), stranger = login("chen");
    jdbc.update("update sys_user set nickname='相同昵称' where username in ('student','chen')");
    long gid =
        jdbc.queryForObject("select id from goods where title='Java Web 开发实战教材'", Long.class);
    long mid =
        buyer
            .data(
                post("/api/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"goodsId\":" + gid + ",\"content\":\"可以面交吗？\"}"))
            .path("id")
            .asLong();
    stranger
        .call(
            patch("/api/messages/" + mid + "/reply")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"冒充回复\"}"))
        .andExpect(status().isForbidden());
    seller
        .call(
            patch("/api/messages/" + mid + "/reply")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"可以面交\"}"))
        .andExpect(status().isOk());
    assertEquals(
        2,
        jdbc.queryForObject(
            "select count(*) from outbox_event where business_id=? and type in"
                + " ('MESSAGE_CREATED','MESSAGE_REPLIED')",
            Integer.class,
            mid));
  }

  @Test
  void otherPrivateListsIgnoreForgedUserHeader() throws Exception {
    Browser b = login("zhou");
    for (JsonNode row : b.data(get("/api/goods/mine").header("X-User-Id", 1)).path("records"))
      assertEquals(3, row.path("sellerId").asLong());
  }

  @Test
  void favoriteDoesNotExposeAnUnlistedGoodsRecord() throws Exception {
    Browser owner = login("student"), other = login("zhou");
    Long gid =
        jdbc.queryForObject(
            "select id from goods where seller_id=1 and status='OFF_SHELF' limit 1", Long.class);
    other.call(put("/api/goods/" + gid + "/favorite")).andExpect(status().isNotFound());
    for (int i = 0; i < 2; i++) {
      JsonNode data = other.data(delete("/api/goods/" + gid + "/favorite"));
      assertFalse(data.has("title"));
      assertFalse(data.has("description"));
      assertFalse(data.path("favorite").asBoolean());
    }
  }
}
