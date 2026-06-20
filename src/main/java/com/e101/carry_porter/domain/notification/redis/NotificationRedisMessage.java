package com.e101.carry_porter.domain.notification.redis;

public record NotificationRedisMessage(
        Long notificationId,
        Long userId
) {
}
