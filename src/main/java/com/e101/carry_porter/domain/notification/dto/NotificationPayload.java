package com.e101.carry_porter.domain.notification.dto;

import com.e101.carry_porter.domain.notification.entity.Notification;

public record NotificationPayload(
        String eventType,
        Long missionId,
        Long userId,
        String message,
        String failureCode
) {

    public static NotificationPayload of(String eventType, Long missionId, Long userId, String message) {
        return new NotificationPayload(eventType, missionId, userId, message, null);
    }

    public static NotificationPayload failure(
            Long missionId,
            Long userId,
            String message,
            String failureCode
    ) {
        return new NotificationPayload("MISSION_FAILED", missionId, userId, message, failureCode);
    }

    public static NotificationPayload from(Notification notification) {
        return new NotificationPayload(
                notification.getEventType(),
                notification.getMissionId(),
                notification.getUserId(),
                notification.getMessage(),
                notification.getFailureCode()
        );
    }
}
