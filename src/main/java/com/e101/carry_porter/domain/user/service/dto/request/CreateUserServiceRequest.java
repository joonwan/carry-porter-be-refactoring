package com.e101.carry_porter.domain.user.service.dto.request;

public record CreateUserServiceRequest(
        String username,
        String password
) {
}
