package com.e101.carry_porter.domain.robot.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RobotInboundPayload(
        @JsonProperty("robot_event_id")
        String robotEventId,
        Long missionId,
        Long userId,
        String failureCode,
        String message
) {
}
