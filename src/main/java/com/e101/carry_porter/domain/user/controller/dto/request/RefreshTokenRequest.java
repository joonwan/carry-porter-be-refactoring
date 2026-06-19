package com.e101.carry_porter.domain.user.controller.dto.request;

import com.e101.carry_porter.domain.user.service.dto.request.RefreshTokenServiceRequest;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken은 비어 있을 수 없습니다.")
        String refreshToken
) {

    public RefreshTokenServiceRequest toServiceRequest() {
        return new RefreshTokenServiceRequest(refreshToken);
    }
}
