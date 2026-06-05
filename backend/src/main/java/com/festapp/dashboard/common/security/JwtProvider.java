// JWT 제공자 - JWT 토큰 생성, 파싱, 검증
package com.festapp.dashboard.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateAccessToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        Date expiryDate = new Date(System.currentTimeMillis() + accessTokenExpiration);

        String token = Jwts.builder()
                .setSubject(String.valueOf(userPrincipal.getUserId()))
                .claim("username", userPrincipal.getUsername())
                .claim("role", userPrincipal.getRole())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        log.debug("Generated access token for user: {} (userId: {})", userPrincipal.getUsername(), userPrincipal.getUserId());
        return token;
    }

    public String generateAccessTokenFromUserId(Long userId, String username, String role) {
        Date expiryDate = new Date(System.currentTimeMillis() + accessTokenExpiration);

        String token = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        log.debug("Generated access token from userId for user: {} (userId: {})", username, userId);
        return token;
    }

    public String generateRefreshToken(Long userId) {
        Date expiryDate = new Date(System.currentTimeMillis() + refreshTokenExpiration);

        String token = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        log.debug("Generated refresh token for userId: {}", userId);
        return token;
    }

    public Long getUserIdFromJWT(String token) {
        return Long.parseLong(getAllClaimsFromToken(token).getSubject());
    }

    public String getUsernameFromJWT(String token) {
        return (String) getAllClaimsFromToken(token).get("username");
    }

    public String getRoleFromJWT(String token) {
        return (String) getAllClaimsFromToken(token).get("role");
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            log.debug("Token validation successful");
            return true;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessTokenExpirationTime() {
        return accessTokenExpiration;
    }
}

