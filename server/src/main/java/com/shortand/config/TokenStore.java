package com.shortand.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {
    private final Map<String, Long> userTokens = new ConcurrentHashMap<>();
    private final Map<String, Long> adminTokens = new ConcurrentHashMap<>();

    public String issueUser(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        userTokens.put(token, userId);
        return token;
    }

    public String issueAdmin(Long adminId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        adminTokens.put(token, adminId);
        return token;
    }

    public Long getUser(String token) {
        return token == null ? null : userTokens.get(token);
    }

    public Long getAdmin(String token) {
        return token == null ? null : adminTokens.get(token);
    }

    public void removeUser(String token) {
        if (token != null) userTokens.remove(token);
    }

    public void removeAdmin(String token) {
        if (token != null) adminTokens.remove(token);
    }
}
