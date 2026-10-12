package com.campus.market.auth;

import java.util.Map;

public class AuthService {
    public static Map<String, Object> view(User u) {
        return Map.of("id", u.getId(), "username", u.getUsername(), "nickname", u.getNickname(), "role",
                u.getRole());
    }
}
