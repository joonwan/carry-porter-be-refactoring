package com.e101.carry_porter.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.mission.entity.MissionStatus;
import com.e101.carry_porter.domain.mission.event.MissionCreatedEvent;
import com.e101.carry_porter.domain.mission.repository.MissionRepository;
import com.e101.carry_porter.domain.mission.service.dto.request.CreateMissionServiceRequest;
import com.e101.carry_porter.domain.mission.service.dto.response.CreateMissionServiceResponse;
import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.domain.user.exception.UserErrorCode;
import com.e101.carry_porter.domain.user.exception.UserException;
import com.e101.carry_porter.domain.user.repository.UserRepository;
import com.e101.carry_porter.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

class MissionServiceTest extends IntegrationTestSupport {

    @Autowired
    private MissionService missionService;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEvents events;

    @Test
    @DisplayName("사용자가 존재하면 미션을 생성하고 CREATED 상태로 저장한다")
    void createMission() {
        // given
        User user = userRepository.save(User.createUser("tester"));
        CreateMissionServiceRequest request = new CreateMissionServiceRequest(user.getId());

        // when
        CreateMissionServiceResponse response = missionService.createMission(request);

        // then
        Mission mission = missionRepository.findById(response.missionId()).orElseThrow();

        assertThat(response.missionId()).isNotNull();
        assertThat(mission.getUser().getId()).isEqualTo(user.getId());
        assertThat(mission.getMissionStatus()).isEqualTo(MissionStatus.CREATED);
        assertThat(mission.getRobot()).isNull();
        assertThat(events.stream(MissionCreatedEvent.class)).hasSize(1);
        assertThat(events.stream(MissionCreatedEvent.class).findFirst()).isPresent()
                .get()
                .extracting(MissionCreatedEvent::missionId, MissionCreatedEvent::userId)
                .containsExactly(response.missionId(), user.getId());
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 UserException을 던진다")
    void createMissionWithInvalidUser() {
        // given
        CreateMissionServiceRequest request = new CreateMissionServiceRequest(9999L);

        // when & then
        assertThatThrownBy(() -> missionService.createMission(request))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
}
