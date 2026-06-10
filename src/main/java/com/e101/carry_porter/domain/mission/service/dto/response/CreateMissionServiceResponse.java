package com.e101.carry_porter.domain.mission.service.dto.response;

import com.e101.carry_porter.domain.mission.entity.Mission;

public record CreateMissionServiceResponse(
        Long missionId
) {

    public static CreateMissionServiceResponse from(Mission mission) {
        return new CreateMissionServiceResponse(mission.getId());
    }
}
