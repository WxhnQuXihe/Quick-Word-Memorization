package com.shorthand.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * 无状态登录 token：HMAC 签名，服务重启后仍然有效。
 * token 格式：base64url(type:userId:exp).base64url(hmac)
 */
@Component
public class TokenStore {
    private static final String HMAC = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    @Value("${app.token-secret:shorthand-dev-secret-change-me}")
    private String secret;

    @Value("${app.token-days:90}")
    private long tokenDays;

    public String issueUser(Long userId) {
        return issue("user", userId);
    }

    public String issueAdmin(Long adminId) {
        return issue("admin", adminId);
    }

    public Long getUser(String token) {
        return parse(token, "user");
    }

    public Long getAdmin(String token) {
        return parse(token, "admin");
    }

    public void removeUser(String token) {
        // 无状态 token：登出由客户端清除；如需服务端强制失效，可改回 token 表。
    }

    public void removeAdmin(String token) {
        // 同上
    }

    private String issue(String type, Long id) {
        long exp = Instant.now().plus(tokenDays, ChronoUnit.DAYS).getEpochSecond();
        String payload = ENCODER.encodeToString((type + ":" + id + ":" + exp)
                .getBytes(StandardCharsets.UTF_8));
        return payload + "." + ENCODER.encodeToString(sign(payload));
    }

    private Long parse(String token, String expectedType) {
        if (token == null || token.isBlank()) return null;
        try {
            int dot = token.indexOf('.');
            if (dot <= 0 || dot == token.length() - 1) return null;
            String payload = token.substring(0, dot);
            String sig = token.substring(dot + 1);
            byte[] expected = sign(payload);
            byte[] actual = DECODER.decode(sig);
            if (!java.security.MessageDigest.isEqual(expected, actual)) return null;
            String decoded = new String(DECODER.decode(payload), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 3 || !expectedType.equals(parts[0])) return null;
            if (Long.parseLong(parts[2]) < Instant.now().getEpochSecond()) return null;
            return Long.parseLong(parts[1]);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("token sign failed", e);
        }
    }
}
