package com.e101.carry_porter.domain.robot.event;

public record RobotDisconnectedMessageReceivedEvent(
        String macAddress
) {
}
