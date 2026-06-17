package com.e101.carry_porter.domain.mission.controller;

import com.e101.carry_porter.domain.mission.service.MissionService;
import com.e101.carry_porter.domain.mission.service.dto.request.CreateMissionServiceRequest;
import com.e101.carry_porter.domain.mission.service.dto.response.CreateMissionServiceResponse;
import com.e101.carry_porter.global.response.ApiResponse;
import com.e101.carry_porter.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateMissionServiceResponse>> createMission(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        CreateMissionServiceResponse response = missionService.createMission(
                new CreateMissionServiceRequest(authenticatedUser.userId())
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("MISSION_CREATED", "미션이 생성되었습니다.", response));
    }
}
