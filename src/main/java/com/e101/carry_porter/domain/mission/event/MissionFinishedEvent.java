package com.e101.carry_porter.domain.mission.event;

public record MissionFinishedEvent(
        Long missionId,
        Long robotId,
        Long userId
) {
}
