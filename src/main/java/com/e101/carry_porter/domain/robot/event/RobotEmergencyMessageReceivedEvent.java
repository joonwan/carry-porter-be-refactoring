package com.e101.carry_porter.domain.robot.event;

public record RobotEmergencyMessageReceivedEvent(
        Long missionId,
        String robotMacAddress,
        Long userId,
        String failureCode,
        String message
) {
}
