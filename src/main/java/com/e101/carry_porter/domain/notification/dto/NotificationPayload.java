package com.e101.carry_porter.domain.notification.dto;

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
}
