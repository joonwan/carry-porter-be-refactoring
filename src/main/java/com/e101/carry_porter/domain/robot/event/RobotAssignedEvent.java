package com.e101.carry_porter.domain.robot.event;

public record RobotAssignedEvent(
        Long missionId,
        Long robotId,
        Long userId
) {
}
