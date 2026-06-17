package com.e101.carry_porter.domain.user.service.dto.response;

public record LoginServiceResponse(
        String accessToken,
        String tokenType
) {

    public static LoginServiceResponse of(String accessToken) {
        return new LoginServiceResponse(accessToken, "Bearer");
    }
}
