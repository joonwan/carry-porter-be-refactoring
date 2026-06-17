package com.e101.carry_porter.domain.user.service.dto.response;

import java.time.OffsetDateTime;

public record LoginServiceResponse(
        String accessToken,
        String tokenType,
        String expiresAt
) {

    public static LoginServiceResponse of(String accessToken, OffsetDateTime expiresAt) {
        return new LoginServiceResponse(accessToken, "Bearer", expiresAt.toString());
    }
}
