package com.e101.carry_porter.domain.mission.event;

public record MissionArrivedEvent(
        Long missionId,
        Long robotId,
        Long userId
) {
}
