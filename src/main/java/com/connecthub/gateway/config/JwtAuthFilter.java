package com.connecthub.gateway.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // Public paths — no JWT needed
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            // Check Authorization header
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Authorization header missing");
            }

            String authHeader = exchange.getRequest()
                    .getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header format");
            }

            String token = authHeader.substring(7);

            // Validate JWT
            try {
                Claims claims = parseToken(token);
                // Add userId to downstream request headers
                exchange = exchange.mutate()
                        .request(r -> r.header("X-User-Id",
                                claims.get("userId", Integer.class).toString()))
                        .build();
            } catch (JwtException e) {
                return onError(exchange, "Invalid or expired token");
            }

            return chain.filter(exchange);
        };
    }

    // Paths that don't need JWT
    private boolean isPublicPath(String path) {
        return path.startsWith("/auth/register")
                || path.startsWith("/auth/login")
                || path.startsWith("/auth/refresh")
                || path.startsWith("/auth/validate")
                || path.startsWith("/oauth2/")          // Google OAuth2 authorization
                || path.startsWith("/login/oauth2/")    // Google OAuth2 callback (Spring default)
                || path.startsWith("/auth/oauth2/")     // Auth service OAuth2 endpoints
                || path.startsWith("/ws/");
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Claims parseToken(String token) {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder()
                        .encodeToString(jwtSecret.getBytes())
        );
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static class Config {
        // Config class — empty for now
    }
}
