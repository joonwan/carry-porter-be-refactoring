package com.e101.carry_porter.domain.mission.event;

public record MissionCreatedEvent(
        Long missionId,
        Long userId
) {
}
