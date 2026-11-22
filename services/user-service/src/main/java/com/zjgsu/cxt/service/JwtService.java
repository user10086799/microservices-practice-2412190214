package com.zjgsu.cxt.service;

import com.zjgsu.cxt.model.ShopUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.ttl-seconds:7200}") long ttlSeconds) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET 长度至少 32 字节");
        }
        this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(bytes);
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(ShopUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getRole())
                .claim("name", user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    public JwtPrincipal requireRole(String authorization, String role) {
        JwtPrincipal principal = parse(authorization);
        if (!role.equals(principal.role())) {
            throw new BusinessException(403, "权限不足");
        }
        return principal;
    }

    public JwtPrincipal parse(String authorization) {
        String token = bearer(authorization);
        if (token == null) {
            throw new BusinessException(401, "未登录");
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Object uid = claims.get("uid");
            if (!(uid instanceof Number number) || claims.getSubject() == null || claims.get("role") == null) {
                throw new BusinessException(401, "未登录");
            }
            return new JwtPrincipal(number.longValue(), claims.getSubject(), String.valueOf(claims.get("role")));
        } catch (BusinessException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(401, "未登录");
        }
    }

    private static String bearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }

    public record JwtPrincipal(long userId, String username, String role) {
    }
}
