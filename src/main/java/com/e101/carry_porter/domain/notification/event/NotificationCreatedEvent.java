package com.e101.carry_porter.domain.notification.event;

public record NotificationCreatedEvent(
        Long notificationId,
        Long userId
) {
}
