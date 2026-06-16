package com.e101.carry_porter.domain.mission.event;

public record MissionFinishedEvent(
        Long missionId,
        String robotMacAddress,
        Long userId
) {
}
