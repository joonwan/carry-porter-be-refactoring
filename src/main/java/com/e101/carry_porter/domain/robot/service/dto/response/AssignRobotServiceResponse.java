package com.e101.carry_porter.domain.robot.service.dto.response;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.robot.entity.Robot;

public record AssignRobotServiceResponse(
        Long missionId,
        Long robotId,
        Long userId
) {

    public static AssignRobotServiceResponse from(Mission mission, Robot robot) {
        return new AssignRobotServiceResponse(
                mission.getId(),
                robot.getId(),
                mission.getUser().getId()
        );
    }
}
