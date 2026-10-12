package com.campus.market;

import com.campus.market.auth.UserMapper;
import com.campus.market.goods.GoodsMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:interaction-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class CampusInteractionTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private UserMapper users;
    @Autowired
    private GoodsMapper goods;

    @Test
    void ownerCanEditAndWithdrawWantButOthersCannot() throws Exception {
        String ownerId = users.findByUsername("student").getId().toString();
        String otherId = users.findByUsername("zhou").getId().toString();
        String payload = "{\"title\":\"求购测试教材\",\"category\":\"教材书籍\",\"budget\":25,\"description\":\"需要完整教材，图书馆面交\"}";
        String response = mvc
                .perform(post("/api/wants").header("X-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PENDING")).andReturn()
                .getResponse().getContentAsString();
        Long wantId = json.readTree(response).path("data").path("id").asLong();
        mvc.perform(put("/api/wants/" + wantId).header("X-User-Id", otherId)
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
        mvc.perform(put("/api/wants/" + wantId).header("X-User-Id", ownerId)
                .contentType(MediaType.APPLICATION_JSON).content(payload.replace("25", "35")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.budget").value(35));
        mvc.perform(patch("/api/wants/" + wantId + "/status").param("value", "INVALID").header("X-User-Id",
                ownerId)).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/wants/" + wantId + "/status").param("value", "WITHDRAWN").header("X-User-Id",
                ownerId)).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
        mvc.perform(put("/api/wants/" + wantId).header("X-User-Id", ownerId)
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isConflict());
        mvc.perform(patch("/api/admin/wants/" + wantId + "/status").param("value", "PUBLISHED")
                .header("X-User-Role", "ADMIN")).andExpect(status().isConflict());
        mvc.perform(post("/api/wants").header("X-User-Id", ownerId).contentType(MediaType.APPLICATION_JSON)
                .content(payload.replace("25", "-1"))).andExpect(status().isBadRequest());
    }

    @Test
    void approvedWantCanBeMarkedFoundAndCannotBeReopened() throws Exception {
        String ownerId = users.findByUsername("zhou").getId().toString();
        JsonNode wants = json.readTree(mvc
                .perform(get("/api/wants").param("onlyMine", "true").param("status", "PUBLISHED")
                        .header("X-User-Id", ownerId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data")
                .path("records");
        Long wantId = null;
        for (JsonNode want : wants) {
            if (want.path("userId").asText().equals(ownerId)
                    && want.path("status").asText().equals("PUBLISHED")) {
                wantId = want.path("id").asLong();
                break;
            }
        }
        org.junit.jupiter.api.Assertions.assertNotNull(wantId);
        mvc.perform(patch("/api/wants/" + wantId + "/status").param("value", "FOUND").header("X-User-Id",
                ownerId)).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("FOUND"));
        mvc.perform(patch("/api/admin/wants/" + wantId + "/status").param("value", "PUBLISHED")
                .header("X-User-Role", "ADMIN")).andExpect(status().isConflict());
    }

    @Test
    void onlySellerCanReplyAndReplyAppearsInBuyerInbox() throws Exception {
        Long sellerId = users.findByUsername("student").getId();
        Long buyerId = users.findByUsername("zhou").getId();
        Long strangerId = users.findByUsername("chen").getId();
        Long goodsId = goods.findMine(sellerId).stream()
                .filter(item -> item.getTitle().equals("Java Web 开发实战教材")).findFirst().orElseThrow().getId();
        String response = mvc
                .perform(post("/api/messages").header("X-User-Id", buyerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goodsId\":" + goodsId + ",\"content\":\"可以图书馆面交吗？\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userName").value("周同学")).andReturn()
                .getResponse().getContentAsString();
        Long messageId = json.readTree(response).path("data").path("id").asLong();
        String reply = "{\"content\":\"可以，下午六点图书馆见。\"}";
        mvc.perform(patch("/api/messages/" + messageId + "/reply").header("X-User-Id", strangerId)
                .contentType(MediaType.APPLICATION_JSON).content(reply)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/messages/" + messageId + "/reply").header("X-User-Id", sellerId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/messages/" + messageId + "/reply").header("X-User-Id", sellerId)
                .contentType(MediaType.APPLICATION_JSON).content(reply)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mvc.perform(get("/api/messages/mine").header("X-User-Id", buyerId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].replyContent").value("可以，下午六点图书馆见。"))
                .andExpect(jsonPath("$.data.records[0].goodsTitle").value("Java Web 开发实战教材"));
        mvc.perform(post("/api/messages").header("X-User-Id", buyerId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"goodsId\":999999,\"content\":\"咨询不存在商品\"}")).andExpect(status().isNotFound());
        mvc.perform(get("/api/messages/mine").header("X-User-Id", users.findByUsername("gu").getId()))
                .andExpect(status().isForbidden());
    }
}
