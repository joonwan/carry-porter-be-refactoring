package com.e101.carry_porter.domain.mission.event;

public record MissionFailedEvent(
        Long missionId,
        Long robotId,
        Long userId,
        String failureCode,
        String message
) {
}
