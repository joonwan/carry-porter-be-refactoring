package com.e101.carry_porter.domain.user.service.dto.request;

public record LoginServiceRequest(
        String username,
        String password
) {
}
