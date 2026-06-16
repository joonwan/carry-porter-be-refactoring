package com.e101.carry_porter.domain.mission.event;

public record MissionArrivedEvent(
        Long missionId,
        String robotMacAddress,
        Long userId
) {
}
