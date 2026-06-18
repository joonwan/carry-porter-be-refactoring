package com.e101.carry_porter.domain.user.service.dto.response;

import java.time.OffsetDateTime;

public record LoginServiceResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        String expiresAt
) {

    public static LoginServiceResponse of(String accessToken, String refreshToken, OffsetDateTime expiresAt) {
        return new LoginServiceResponse(accessToken, refreshToken, "Bearer", expiresAt.toString());
    }
}
