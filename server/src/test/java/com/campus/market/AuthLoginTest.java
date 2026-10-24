package com.campus.market;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class AuthLoginTest extends MarketTestSupport {
  @Autowired JdbcTemplate jdbc;

  @Test
  void forgedIdentityAndMissingCsrfAreRejected() throws Exception {
    mvc.perform(get("/api/admin/users").header("X-User-Role", "ADMIN").header("X-User-Id", 2))
        .andExpect(status().isUnauthorized());
    login("student")
        .call(get("/api/admin/users").header("X-User-Role", "ADMIN"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
    Browser b = login("student");
    b.csrf = null;
    b.call(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void loginRotatesSessionAndNeverReturnsPasswordHash() throws Exception {
    Browser b = new Browser();
    b.token();
    String before = b.cookie.getValue();
    b.data(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin\",\"password\":\"admin123\"}"));
    assertNotEquals(before, b.cookie.getValue());
    assertFalse(b.data(get("/api/auth/me")).has("passwordHash"));
    b.token();
    b.call(post("/api/auth/logout")).andExpect(status().isOk());
    b.call(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void registrationCannotEscalateRoleAndCsrfChangesAfterLogin() throws Exception {
    Browser b = new Browser();
    b.token();
    String old = b.csrf;
    String name = "reg_" + System.nanoTime();
    assertEquals(
        "USER",
        b.data(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\""
                            + name
                            + "\",\"password\":\"test123456\",\"nickname\":\"测试同学\",\"role\":\"ADMIN\"}"))
            .path("role")
            .asText());
    b.call(post("/api/auth/logout")).andExpect(status().isForbidden());
    b.token();
    assertNotEquals(old, b.csrf);
    b.call(post("/api/auth/logout")).andExpect(status().isOk());
  }

  @Test
  @Transactional
  void disabledAndVersionChangedUsersLoseExistingSessions() throws Exception {
    Browser b = login("student");
    jdbc.update("update sys_user set auth_version=auth_version+1 where username='student'");
    b.call(get("/api/auth/me")).andExpect(status().isUnauthorized());
    b = login("zhou");
    jdbc.update("update sys_user set status=0 where username='zhou'");
    b.call(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void wrongPasswordAndInvalidInputsHaveUsefulStatuses() throws Exception {
    Browser b = new Browser();
    b.token();
    b.call(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
    b.call(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":null}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void passwordChangeInvalidatesOtherSessions() throws Exception {
    Browser first = new Browser();
    first.token();
    String name = "pwd_" + System.nanoTime();
    first.data(
        post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"username\":\"" + name + "\",\"password\":\"123456\",\"nickname\":\"改密测试\"}"));
    first.token();
    Browser other = login(name);
    first
        .call(
            put("/api/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"123456\",\"newPassword\":\"changed123\"}"))
        .andExpect(status().isOk());
    other.call(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  @Transactional
  void roleChangeInvalidatesOldSessionAndAdminStatusIsWhitelisted() throws Exception {
    Browser admin = login("admin"), user = login("demo24");
    Long uid = jdbc.queryForObject("select id from sys_user where username='demo24'", Long.class);
    admin
        .call(
            patch("/api/admin/users/" + uid + "/role")
                .param("value", "ADMIN")
                .param("reason", "测试角色调整"))
        .andExpect(status().isOk());
    user.call(get("/api/auth/me")).andExpect(status().isUnauthorized());
    admin
        .call(patch("/api/admin/orders/1/status").param("value", "SOLD").param("reason", "非法状态验证"))
        .andExpect(status().isBadRequest());
  }
}
