package com.e101.carry_porter.domain.user.service.dto.response;

import com.e101.carry_porter.domain.user.entity.User;

public record CreateUserServiceResponse(
        Long userId,
        String username
) {

    public static CreateUserServiceResponse from(User user) {
        return new CreateUserServiceResponse(user.getId(), user.getUsername());
    }
}
