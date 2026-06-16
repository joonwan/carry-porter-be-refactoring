package com.e101.carry_porter.domain.mission.event;

public record MissionFailedEvent(
        Long missionId,
        String robotMacAddress,
        Long userId,
        String failureCode,
        String message
) {
}
