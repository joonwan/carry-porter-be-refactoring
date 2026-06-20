package com.e101.carry_porter.domain.robot.event;

public record RobotConnectedMessageReceivedEvent(
        String robotEventId,
        String macAddress
) {
}
