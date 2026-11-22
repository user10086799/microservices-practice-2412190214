package com.zjgsu.cxt.gateway;

import com.zjgsu.cxt.gateway.AccessPolicy.Decision;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuthFilter.class);

    private final SecretKey key;

    public GatewayAuthFilter(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET 长度至少 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() == null ? "" : request.getMethod().name();
        ServerHttpRequest.Builder builder = request.mutate();
        builder.headers(headers -> {
            headers.remove("X-User-Id");
            headers.remove("X-User-Name");
            headers.remove("X-User-Role");
        });
        Decision decision = AccessPolicy.decide(method, path);
        if (decision == Decision.PUBLIC) {
            logForward(method, path);
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }
        Claims claims = parse(request.getHeaders().getFirst("Authorization"));
        if (claims == null) {
            log.info("网关拒绝未登录 {} {}", method, path);
            return deny(exchange, HttpStatus.UNAUTHORIZED, "未登录");
        }
        String role = String.valueOf(claims.get("role"));
        if (decision == Decision.ADMIN && !"ADMIN".equals(role)) {
            log.info("网关拒绝权限不足 {} {}", method, path);
            return deny(exchange, HttpStatus.FORBIDDEN, "权限不足");
        }
        logForward(method, path);
        Object uid = claims.get("uid");
        builder.header("X-User-Id", uid == null ? "" : String.valueOf(uid));
        builder.header("X-User-Name", claims.getSubject() == null ? "" : claims.getSubject());
        builder.header("X-User-Role", role);
        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    private static void logForward(String method, String path) {
        if (path.startsWith("/api/")) {
            log.info("网关按服务名转发 {} {} {}", AccessPolicy.targetService(path), method, path);
        }
    }

    private Claims parse(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return null;
        }
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException exception) {
            return null;
        }
    }

    private static Mono<Void> deny(ServerWebExchange exchange, HttpStatus status, String message) {
        byte[] bytes = ("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
