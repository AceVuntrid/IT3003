package com.university.assets.security;

import com.university.assets.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessExpiry;

    public JwtService(AppProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.accessExpiry = Duration.ofMinutes(properties.jwt().accessExpiryMinutes());
    }

    public String createAccessToken(SecurityUser user) {
        Instant now = Instant.now();
        List<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getUsername())
                .claim("authorities", authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessExpiry)))
                .signWith(key)
                .compact();
    }

    /** Returns the user id from a valid token, or null when invalid/expired. */
    public UUID validateAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
            return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
