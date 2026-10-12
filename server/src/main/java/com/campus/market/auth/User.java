package com.campus.market.auth;

public class User {
    private Long id;
    private String username, passwordHash, nickname, role;
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        username = v;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String v) {
        passwordHash = v;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String v) {
        nickname = v;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String v) {
        role = v;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer v) {
        status = v;
    }
}
