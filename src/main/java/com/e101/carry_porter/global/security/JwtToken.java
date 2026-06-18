package com.e101.carry_porter.global.security;

import java.time.OffsetDateTime;

public record JwtToken(
        String accessToken,
        String refreshToken,
        OffsetDateTime expiresAt
) {
}
