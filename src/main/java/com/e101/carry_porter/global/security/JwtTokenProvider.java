package com.e101.carry_porter.global.security;

import com.e101.carry_porter.global.config.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public JwtToken createAccessToken(Long userId, String username) {
        Date now = new Date();
        Date accessTokenExpiration = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());
        Date refreshTokenExpiration = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))        // token 소유 주체
                .claim("username", username)     // payload
                .issuedAt(now)
                .expiration(accessTokenExpiration)
                .signWith(getSigningKey())
                .compact();

        String refreshToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(refreshTokenExpiration)
                .signWith(getSigningKey())
                .compact();

        return new JwtToken(
                accessToken,
                refreshToken,
                OffsetDateTime.ofInstant(accessTokenExpiration.toInstant(), ZoneOffset.UTC)
        );
    }

    public boolean validateToken(String accessToken) {
        try {
            parseClaims(accessToken);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public AuthenticatedUser getAuthenticatedUser(String accessToken) {
        Claims claims = parseClaims(accessToken);

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);

        return new AuthenticatedUser(userId, username);
    }

    private Claims parseClaims(String accessToken) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // 검증 키 지정
                .build()
                .parseSignedClaims(accessToken) // 검증 및 parsing
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
