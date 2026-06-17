package com.e101.carry_porter.domain.robot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.mission.entity.MissionStatus;
import com.e101.carry_porter.domain.mission.exception.MissionErrorCode;
import com.e101.carry_porter.domain.mission.exception.MissionException;
import com.e101.carry_porter.domain.mission.repository.MissionRepository;
import com.e101.carry_porter.domain.robot.entity.Robot;
import com.e101.carry_porter.domain.robot.entity.RobotStatus;
import com.e101.carry_porter.domain.robot.event.RobotAssignedEvent;
import com.e101.carry_porter.domain.robot.exception.RobotErrorCode;
import com.e101.carry_porter.domain.robot.exception.RobotException;
import com.e101.carry_porter.domain.robot.repository.RobotRepository;
import com.e101.carry_porter.domain.robot.service.dto.request.AssignRobotServiceRequest;
import com.e101.carry_porter.domain.robot.service.dto.response.AssignRobotServiceResponse;
import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.domain.user.repository.UserRepository;
import com.e101.carry_porter.support.TransactionalIntegrationTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

@Slf4j
class RobotServiceTest extends TransactionalIntegrationTestSupport {

    @Autowired
    private RobotService robotService;

    @Autowired
    private RobotRepository robotRepository;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEvents events;

    @Test
    @DisplayName("존재하지 않는 macAddress로 연결되면 새 로봇을 생성하고 IDLE 상태로 저장한다")
    void registerOrReconnectWithNewRobot() {
        // given
        String macAddress = "AA:BB:CC:DD:EE:10";

        // when
        robotService.registerOrReconnect(macAddress);

        // then
        Robot registeredRobot = robotRepository.findByMacAddress(macAddress).orElseThrow();

        assertThat(registeredRobot.getMacAddress()).isEqualTo(macAddress);
        assertThat(registeredRobot.getRobotStatus()).isEqualTo(RobotStatus.IDLE);
    }

    @Test
    @DisplayName("OFFLINE 상태의 로봇이 다시 연결되면 IDLE 상태로 변경한다")
    void registerOrReconnectWithOfflineRobot() {
        // given
        Robot offlineRobot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:11"));
        offlineRobot.toOffline();

        // when
        robotService.registerOrReconnect(offlineRobot.getMacAddress());

        // then
        Robot reconnectedRobot = robotRepository.findByMacAddress(offlineRobot.getMacAddress()).orElseThrow();

        assertThat(reconnectedRobot.getId()).isEqualTo(offlineRobot.getId());
        assertThat(reconnectedRobot.getRobotStatus()).isEqualTo(RobotStatus.IDLE);
    }

    @Test
    @DisplayName("IDLE 상태의 로봇이 다시 연결되면 상태를 그대로 유지한다")
    void registerOrReconnectWithIdleRobot() {
        // given
        Robot idleRobot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:12"));

        // when
        robotService.registerOrReconnect(idleRobot.getMacAddress());

        // then
        Robot reconnectedRobot = robotRepository.findByMacAddress(idleRobot.getMacAddress()).orElseThrow();

        assertThat(reconnectedRobot.getId()).isEqualTo(idleRobot.getId());
        assertThat(reconnectedRobot.getRobotStatus()).isEqualTo(RobotStatus.IDLE);
    }

    @Test
    @DisplayName("BUSY 상태의 로봇이 다시 연결되면 상태를 그대로 유지한다")
    void registerOrReconnectWithBusyRobot() {
        // given
        Robot busyRobot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:13"));
        busyRobot.toBusy();

        // when
        robotService.registerOrReconnect(busyRobot.getMacAddress());

        // then
        Robot reconnectedRobot = robotRepository.findByMacAddress(busyRobot.getMacAddress()).orElseThrow();

        assertThat(reconnectedRobot.getId()).isEqualTo(busyRobot.getId());
        assertThat(reconnectedRobot.getRobotStatus()).isEqualTo(RobotStatus.BUSY);
    }

    @Test
    @DisplayName("CREATED 상태의 미션에 IDLE 로봇을 배정하고 RobotAssignedEvent를 발행한다")
    void assignRobot() {
        // given
        log.info("assign robot test 시작");
        User user = userRepository.save(User.createUser("tester"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:01"));
        AssignRobotServiceRequest request = new AssignRobotServiceRequest(mission.getId());

        // when
        AssignRobotServiceResponse response = robotService.assignRobot(request);

        // then
        Mission assignedMission = missionRepository.findById(mission.getId()).orElseThrow();
        Robot assignedRobot = robotRepository.findById(robot.getId()).orElseThrow();

        assertThat(response.missionId()).isEqualTo(mission.getId());
        assertThat(response.robotId()).isEqualTo(robot.getId());
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(assignedMission.getMissionStatus()).isEqualTo(MissionStatus.ASSIGNED);
        assertThat(assignedMission.getRobot()).isNotNull();
        assertThat(assignedMission.getRobot().getId()).isEqualTo(robot.getId());
        assertThat(assignedRobot.getRobotStatus()).isEqualTo(RobotStatus.BUSY);
        assertThat(events.stream(RobotAssignedEvent.class)).hasSize(1);
        assertThat(events.stream(RobotAssignedEvent.class).findFirst()).isPresent()
                .get()
                .extracting(RobotAssignedEvent::missionId, RobotAssignedEvent::robotId, RobotAssignedEvent::userId)
                .containsExactly(mission.getId(), robot.getId(), user.getId());
    }

    @Test
    @DisplayName("미션이 CREATED 상태가 아니면 MissionException을 던진다")
    void assignRobotWithInvalidMissionStatus() {
        // given
        log.info("assignRobotWithInvalidMissionStatus test 시작");
        User user = userRepository.save(User.createUser("tester2"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        Robot firstRobot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:02"));
        robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:03"));
        mission.assignRobot(firstRobot);

        AssignRobotServiceRequest request = new AssignRobotServiceRequest(mission.getId());

        // when & then
        assertThatThrownBy(() -> robotService.assignRobot(request))
                .isInstanceOf(MissionException.class)
                .extracting(exception -> ((MissionException) exception).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_MISSION_STATUS);
    }

    @Test
    @DisplayName("배정 가능한 로봇이 없으면 RobotException을 던진다")
    void assignRobotWithoutAvailableRobot() {
        // given
        log.info("assignRobotWithoutAvailableRobot test 시작");
        User user = userRepository.save(User.createUser("tester3"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        Robot busyRobot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:04"));
        busyRobot.toBusy();

        AssignRobotServiceRequest request = new AssignRobotServiceRequest(mission.getId());

        // when & then
        assertThatThrownBy(() -> robotService.assignRobot(request))
                .isInstanceOf(RobotException.class)
                .extracting(exception -> ((RobotException) exception).getErrorCode())
                .isEqualTo(RobotErrorCode.AVAILABLE_ROBOT_NOT_FOUND);
    }
}
