package com.e101.carry_porter.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.mission.entity.MissionStatus;
import com.e101.carry_porter.domain.mission.event.MissionArrivedEvent;
import com.e101.carry_porter.domain.mission.event.MissionCreatedEvent;
import com.e101.carry_porter.domain.mission.event.MissionStartedEvent;
import com.e101.carry_porter.domain.mission.exception.MissionErrorCode;
import com.e101.carry_porter.domain.mission.exception.MissionException;
import com.e101.carry_porter.domain.mission.repository.MissionRepository;
import com.e101.carry_porter.domain.mission.service.dto.request.CreateMissionServiceRequest;
import com.e101.carry_porter.domain.mission.service.dto.response.CreateMissionServiceResponse;
import com.e101.carry_porter.domain.robot.entity.Robot;
import com.e101.carry_porter.domain.robot.repository.RobotRepository;
import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.domain.user.exception.UserErrorCode;
import com.e101.carry_porter.domain.user.exception.UserException;
import com.e101.carry_porter.domain.user.repository.UserRepository;
import com.e101.carry_porter.support.TransactionalIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

class MissionServiceTest extends TransactionalIntegrationTestSupport {

    @Autowired
    private MissionService missionService;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RobotRepository robotRepository;

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

    @Test
    @DisplayName("배정된 미션을 시작 처리하면 DISPATCHED 상태로 변경하고 MissionStartedEvent를 발행한다")
    void dispatch() {
        // given
        User user = userRepository.save(User.createUser("dispatch-user"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:21"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);

        // when
        missionService.dispatch(mission.getId(), robot.getId(), user.getId());

        // then
        Mission dispatchedMission = missionRepository.findById(mission.getId()).orElseThrow();

        assertThat(dispatchedMission.getMissionStatus()).isEqualTo(MissionStatus.DISPATCHED);
        assertThat(events.stream(MissionStartedEvent.class)).hasSize(1);
        assertThat(events.stream(MissionStartedEvent.class).findFirst()).isPresent()
                .get()
                .extracting(
                        MissionStartedEvent::missionId,
                        MissionStartedEvent::robotId,
                        MissionStartedEvent::userId,
                        MissionStartedEvent::robotMacAddress
                )
                .containsExactly(mission.getId(), robot.getId(), user.getId(), robot.getMacAddress());
    }

    @Test
    @DisplayName("미션의 사용자나 로봇이 일치하지 않으면 MissionException을 던진다")
    void dispatchWithInvalidDispatchTarget() {
        // given
        User user = userRepository.save(User.createUser("dispatch-user-2"));
        User anotherUser = userRepository.save(User.createUser("dispatch-user-3"));
        Robot assignedRobot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:22"));
        Robot anotherRobot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:23"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(assignedRobot);

        // when & then
        assertThatThrownBy(() -> missionService.dispatch(mission.getId(), anotherRobot.getId(), anotherUser.getId()))
                .isInstanceOf(MissionException.class)
                .extracting(exception -> ((MissionException) exception).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_MISSION_STATUS);
    }

    @Test
    @DisplayName("미션 상태가 ASSIGNED가 아니면 MissionException을 던진다")
    void dispatchWithInvalidMissionStatus() {
        // given
        User user = userRepository.save(User.createUser("dispatch-user-4"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:24"));
        Mission mission = missionRepository.save(Mission.createMission(user));

        // when & then
        assertThatThrownBy(() -> missionService.dispatch(mission.getId(), robot.getId(), user.getId()))
                .isInstanceOf(MissionException.class)
                .extracting(exception -> ((MissionException) exception).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_MISSION_STATUS);
    }

    @Test
    @DisplayName("이동 중인 미션을 도착 처리하면 ARRIVED 상태로 변경한다")
    void arrive() {
        // given
        User user = userRepository.save(User.createUser("arrive-user"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:31"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();

        // when
        missionService.arrive(mission.getId(), robot.getMacAddress(), user.getId());

        // then
        Mission arrivedMission = missionRepository.findById(mission.getId()).orElseThrow();

        assertThat(arrivedMission.getMissionStatus()).isEqualTo(MissionStatus.ARRIVED);
    }

    @Test
    @DisplayName("도착 처리 시 미션의 사용자나 로봇 mac address가 일치하지 않으면 MissionException을 던진다")
    void arriveWithInvalidArrivalTarget() {
        // given
        User user = userRepository.save(User.createUser("arrive-user-2"));
        User anotherUser = userRepository.save(User.createUser("arrive-user-3"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:32"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();

        // when & then
        assertThatThrownBy(() -> missionService.arrive(mission.getId(), "AA:BB:CC:DD:EE:99", anotherUser.getId()))
                .isInstanceOf(MissionException.class)
                .extracting(exception -> ((MissionException) exception).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_MISSION_STATUS);
    }

    @Test
    @DisplayName("미션 상태가 DISPATCHED가 아니면 도착 처리 시 MissionException을 던진다")
    void arriveWithInvalidMissionStatus() {
        // given
        User user = userRepository.save(User.createUser("arrive-user-4"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:33"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);

        // when & then
        assertThatThrownBy(() -> missionService.arrive(mission.getId(), robot.getMacAddress(), user.getId()))
                .isInstanceOf(MissionException.class)
                .extracting(exception -> ((MissionException) exception).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_MISSION_STATUS);
    }
}
