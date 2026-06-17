package com.e101.carry_porter.domain.robot.event;

public record RobotAssignmentFailedEvent(
        Long missionId,
        Long userId,
        String failureCode,
        String message
) {
}
