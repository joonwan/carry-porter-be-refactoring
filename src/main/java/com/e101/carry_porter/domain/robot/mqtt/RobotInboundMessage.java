package com.e101.carry_porter.domain.robot.mqtt;

public record RobotInboundMessage(
        String macAddress,
        String eventName,
        RobotInboundPayload payload
) {
}
