package com.e101.carry_porter.domain.robot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.mission.entity.MissionStatus;
import com.e101.carry_porter.domain.mission.event.MissionFailedEvent;
import com.e101.carry_porter.domain.mission.exception.MissionErrorCode;
import com.e101.carry_porter.domain.mission.exception.MissionException;
import com.e101.carry_porter.domain.mission.repository.MissionRepository;
import com.e101.carry_porter.domain.robot.entity.ProcessedRobotEvent;
import com.e101.carry_porter.domain.robot.entity.Robot;
import com.e101.carry_porter.domain.robot.entity.RobotStatus;
import com.e101.carry_porter.domain.robot.event.RobotAssignedEvent;
import com.e101.carry_porter.domain.robot.event.RobotAssignmentFailedEvent;
import com.e101.carry_porter.domain.robot.exception.RobotErrorCode;
import com.e101.carry_porter.domain.robot.exception.RobotException;
import com.e101.carry_porter.domain.robot.repository.ProcessedRobotEventRepository;
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
    private ProcessedRobotEventRepository processedRobotEventRepository;

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
        String robotEventId = "robot-event-register-1";
        String macAddress = "AA:BB:CC:DD:EE:10";

        // when
        robotService.registerOrReconnect(robotEventId, macAddress);

        // then
        Robot registeredRobot = robotRepository.findByMacAddress(macAddress).orElseThrow();

        assertThat(registeredRobot.getMacAddress()).isEqualTo(macAddress);
        assertThat(registeredRobot.getRobotStatus()).isEqualTo(RobotStatus.IDLE);
        assertThat(processedRobotEventRepository.existsByRobotEventId(robotEventId)).isTrue();
    }

    @Test
    @DisplayName("OFFLINE 상태의 로봇이 다시 연결되면 IDLE 상태로 변경한다")
    void registerOrReconnectWithOfflineRobot() {
        // given
        String robotEventId = "robot-event-register-2";
        Robot offlineRobot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:11"));
        offlineRobot.toOffline();

        // when
        robotService.registerOrReconnect(robotEventId, offlineRobot.getMacAddress());

        // then
        Robot reconnectedRobot = robotRepository.findByMacAddress(offlineRobot.getMacAddress()).orElseThrow();

        assertThat(reconnectedRobot.getId()).isEqualTo(offlineRobot.getId());
        assertThat(reconnectedRobot.getRobotStatus()).isEqualTo(RobotStatus.IDLE);
    }

    @Test
    @DisplayName("IDLE 상태의 로봇이 다시 연결되면 상태를 그대로 유지한다")
    void registerOrReconnectWithIdleRobot() {
        // given
        String robotEventId = "robot-event-register-3";
        Robot idleRobot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:12"));

        // when
        robotService.registerOrReconnect(robotEventId, idleRobot.getMacAddress());

        // then
        Robot reconnectedRobot = robotRepository.findByMacAddress(idleRobot.getMacAddress()).orElseThrow();

        assertThat(reconnectedRobot.getId()).isEqualTo(idleRobot.getId());
        assertThat(reconnectedRobot.getRobotStatus()).isEqualTo(RobotStatus.IDLE);
    }

    @Test
    @DisplayName("BUSY 상태의 로봇이 다시 연결되면 상태를 그대로 유지한다")
    void registerOrReconnectWithBusyRobot() {
        // given
        String robotEventId = "robot-event-register-4";
        Robot busyRobot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:13"));
        busyRobot.toBusy();

        // when
        robotService.registerOrReconnect(robotEventId, busyRobot.getMacAddress());

        // then
        Robot reconnectedRobot = robotRepository.findByMacAddress(busyRobot.getMacAddress()).orElseThrow();

        assertThat(reconnectedRobot.getId()).isEqualTo(busyRobot.getId());
        assertThat(reconnectedRobot.getRobotStatus()).isEqualTo(RobotStatus.BUSY);
    }

    @Test
    @DisplayName("이미 처리된 robotEventId로 로봇 연결 메시지가 다시 들어오면 RobotException을 던진다")
    void registerOrReconnectWithDuplicateRobotEvent() {
        // given
        String robotEventId = "robot-event-register-duplicate-1";
        String macAddress = "AA:BB:CC:DD:EE:17";
        processedRobotEventRepository.saveAndFlush(
                ProcessedRobotEvent.create(robotEventId, macAddress)
        );

        // when & then
        assertThatThrownBy(() -> robotService.registerOrReconnect(robotEventId, macAddress))
                .isInstanceOf(RobotException.class)
                .extracting(exception -> ((RobotException) exception).getErrorCode())
                .isEqualTo(RobotErrorCode.DUPLICATE_ROBOT_EVENT);
    }

    @Test
    @DisplayName("로봇 연결이 끊기면 상태를 OFFLINE으로 변경한다")
    void disconnect() {
        // given
        String robotEventId = "robot-event-disconnect-1";
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:14"));

        // when
        robotService.disconnect(robotEventId, robot.getMacAddress());

        // then
        Robot disconnectedRobot = robotRepository.findByMacAddress(robot.getMacAddress()).orElseThrow();

        assertThat(disconnectedRobot.getRobotStatus()).isEqualTo(RobotStatus.OFFLINE);
        assertThat(processedRobotEventRepository.existsByRobotEventId(robotEventId)).isTrue();
    }

    @Test
    @DisplayName("BUSY 상태의 로봇 연결이 끊겨도 상태를 OFFLINE으로 변경한다")
    void disconnectWithBusyRobot() {
        // given
        String robotEventId = "robot-event-disconnect-2";
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:15"));
        robot.toBusy();

        // when
        robotService.disconnect(robotEventId, robot.getMacAddress());

        // then
        Robot disconnectedRobot = robotRepository.findByMacAddress(robot.getMacAddress()).orElseThrow();

        assertThat(disconnectedRobot.getRobotStatus()).isEqualTo(RobotStatus.OFFLINE);
    }

    @Test
    @DisplayName("진행 중인 미션의 로봇 연결이 끊기면 미션을 FAILED로 변경하고 MissionFailedEvent를 발행한다")
    void disconnectWithActiveMission() {
        // given
        String robotEventId = "robot-event-disconnect-3";
        User user = userRepository.save(User.createUser("disconnect-user", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:16"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();

        // when
        robotService.disconnect(robotEventId, robot.getMacAddress());

        // then
        Mission failedMission = missionRepository.findById(mission.getId()).orElseThrow();
        Robot disconnectedRobot = robotRepository.findByMacAddress(robot.getMacAddress()).orElseThrow();

        assertThat(failedMission.getMissionStatus()).isEqualTo(MissionStatus.FAILED);
        assertThat(disconnectedRobot.getRobotStatus()).isEqualTo(RobotStatus.OFFLINE);
        assertThat(processedRobotEventRepository.existsByRobotEventId(robotEventId)).isTrue();
        assertThat(events.stream(MissionFailedEvent.class)).hasSize(1);
        assertThat(events.stream(MissionFailedEvent.class).findFirst()).isPresent()
                .get()
                .extracting(
                        MissionFailedEvent::missionId,
                        MissionFailedEvent::robotMacAddress,
                        MissionFailedEvent::userId,
                        MissionFailedEvent::failureCode,
                        MissionFailedEvent::message
                )
                .containsExactly(
                        mission.getId(),
                        robot.getMacAddress(),
                        user.getId(),
                        RobotErrorCode.ROBOT_DISCONNECTED.getCode(),
                        RobotErrorCode.ROBOT_DISCONNECTED.getMessage()
                );
    }

    @Test
    @DisplayName("이미 처리된 robotEventId로 로봇 연결 끊김 메시지가 다시 들어오면 RobotException을 던진다")
    void disconnectWithDuplicateRobotEvent() {
        // given
        String robotEventId = "robot-event-disconnect-duplicate-1";
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:18"));
        processedRobotEventRepository.saveAndFlush(
                ProcessedRobotEvent.create(robotEventId, robot.getMacAddress())
        );

        // when & then
        assertThatThrownBy(() -> robotService.disconnect(robotEventId, robot.getMacAddress()))
                .isInstanceOf(RobotException.class)
                .extracting(exception -> ((RobotException) exception).getErrorCode())
                .isEqualTo(RobotErrorCode.DUPLICATE_ROBOT_EVENT);
    }

    @Test
    @DisplayName("CREATED 상태의 미션에 IDLE 로봇을 배정하고 RobotAssignedEvent를 발행한다")
    void assignRobot() {
        // given
        log.info("assign robot test 시작");
        User user = userRepository.save(User.createUser("tester", "password"));
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
        User user = userRepository.save(User.createUser("tester2", "password"));
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
        User user = userRepository.save(User.createUser("tester3", "password"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        Robot busyRobot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:04"));
        busyRobot.toBusy();

        AssignRobotServiceRequest request = new AssignRobotServiceRequest(mission.getId());

        // when & then
        assertThatThrownBy(() -> robotService.assignRobot(request))
                .isInstanceOf(RobotException.class)
                .extracting(exception -> ((RobotException) exception).getErrorCode())
                .isEqualTo(RobotErrorCode.AVAILABLE_ROBOT_NOT_FOUND);

        assertThat(events.stream(RobotAssignmentFailedEvent.class)).hasSize(1);
        assertThat(events.stream(RobotAssignmentFailedEvent.class).findFirst()).isPresent()
                .get()
                .extracting(
                        RobotAssignmentFailedEvent::missionId,
                        RobotAssignmentFailedEvent::userId,
                        RobotAssignmentFailedEvent::failureCode,
                        RobotAssignmentFailedEvent::message
                )
                .containsExactly(
                        mission.getId(),
                        user.getId(),
                        RobotErrorCode.AVAILABLE_ROBOT_NOT_FOUND.getCode(),
                        RobotErrorCode.AVAILABLE_ROBOT_NOT_FOUND.getMessage()
                );
    }
}
