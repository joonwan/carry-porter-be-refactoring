package com.e101.carry_porter.domain.robot.event;

public record RobotReturnedMessageReceivedEvent(
        Long missionId,
        String robotEventId,
        String robotMacAddress,
        Long userId
) {
}
