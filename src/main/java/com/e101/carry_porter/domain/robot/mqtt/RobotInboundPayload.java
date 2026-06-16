package com.e101.carry_porter.domain.robot.mqtt;

public record RobotInboundPayload(
        Long missionId,
        Long userId,
        String failureCode,
        String message
) {
}
