package com.e101.carry_porter.domain.mission.event;

public record MissionReturnStartedEvent(
        Long missionId,
        Long robotId,
        Long userId,
        String robotMacAddress
) {
}
