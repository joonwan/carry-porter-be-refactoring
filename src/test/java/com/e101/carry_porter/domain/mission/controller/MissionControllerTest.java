package com.e101.carry_porter.domain.mission.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.e101.carry_porter.domain.mission.service.MissionService;
import com.e101.carry_porter.domain.mission.service.dto.response.CreateMissionServiceResponse;
import com.e101.carry_porter.global.security.AuthenticatedUser;
import com.e101.carry_porter.support.RestControllerTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(MissionController.class)
class MissionControllerTest extends RestControllerTestSupport {

    @MockitoBean
    private MissionService missionService;

    @Test
    @DisplayName("인증된 사용자가 미션 생성 요청을 하면 201 응답과 missionId를 반환한다")
    void createMission() throws Exception {
        // given
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "mission-user");
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of()
        );

        given(missionService.createMission(any()))
                .willReturn(new CreateMissionServiceResponse(10L));

        // when & then
        mockMvc.perform(post("/api/missions")
                        .with(authentication(authenticationToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MISSION_CREATED"))
                .andExpect(jsonPath("$.message").value("미션이 생성되었습니다."))
                .andExpect(jsonPath("$.data.missionId").value(10L));
    }

    @Test
    @DisplayName("인증된 사용자가 복귀 시작 요청을 하면 200 응답을 반환한다")
    void returnStart() throws Exception {
        // given
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "mission-user");
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of()
        );

        // when & then
        mockMvc.perform(post("/api/missions/{missionId}/return", 10L)
                        .with(authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MISSION_RETURN_STARTED"))
                .andExpect(jsonPath("$.message").value("로봇 복귀를 시작했습니다."));

        verify(missionService).returnStart(10L, 1L);
    }
}
