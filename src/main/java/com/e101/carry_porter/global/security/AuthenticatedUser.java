package com.e101.carry_porter.global.security;

public record AuthenticatedUser(
        Long userId,
        String username
) {
}
