package com.e101.carry_porter.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.mission.entity.MissionStatus;
import com.e101.carry_porter.domain.mission.event.MissionCreatedEvent;
import com.e101.carry_porter.domain.mission.event.MissionFinishedEvent;
import com.e101.carry_porter.domain.mission.event.MissionReturnStartedEvent;
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
        User user = userRepository.save(User.createUser("tester", "password"));
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
        User user = userRepository.save(User.createUser("dispatch-user", "password"));
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
        User user = userRepository.save(User.createUser("dispatch-user-2", "password"));
        User anotherUser = userRepository.save(User.createUser("dispatch-user-3", "password"));
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
        User user = userRepository.save(User.createUser("dispatch-user-4", "password"));
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
        User user = userRepository.save(User.createUser("arrive-user", "password"));
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
    @DisplayName("이미 ARRIVED 상태인 미션에 도착 메시지가 다시 와도 예외 없이 그대로 유지한다")
    void arriveWithAlreadyArrivedMissionStatus() {
        // given
        User user = userRepository.save(User.createUser("arrive-user-duplicate", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:34"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();
        mission.arrive();

        // when
        missionService.arrive(mission.getId(), robot.getMacAddress(), user.getId());

        // then
        Mission unchangedMission = missionRepository.findById(mission.getId()).orElseThrow();

        assertThat(unchangedMission.getMissionStatus()).isEqualTo(MissionStatus.ARRIVED);
        assertThat(unchangedMission.getRobot().getRobotStatus()).isEqualTo(com.e101.carry_porter.domain.robot.entity.RobotStatus.BUSY);
    }

    @Test
    @DisplayName("이미 RETURNING 상태인 미션에 늦게 도착 메시지가 와도 상태를 덮어쓰지 않는다")
    void arriveWithReturningMissionStatus() {
        // given
        User user = userRepository.save(User.createUser("arrive-user-returning", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:35"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();
        mission.arrive();
        mission.startReturning();

        // when
        missionService.arrive(mission.getId(), robot.getMacAddress(), user.getId());

        // then
        Mission unchangedMission = missionRepository.findById(mission.getId()).orElseThrow();

        assertThat(unchangedMission.getMissionStatus()).isEqualTo(MissionStatus.RETURNING);
    }

    @Test
    @DisplayName("도착 처리 시 미션의 사용자나 로봇 mac address가 일치하지 않으면 MissionException을 던진다")
    void arriveWithInvalidArrivalTarget() {
        // given
        User user = userRepository.save(User.createUser("arrive-user-2", "password"));
        User anotherUser = userRepository.save(User.createUser("arrive-user-3", "password"));
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
        User user = userRepository.save(User.createUser("arrive-user-4", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:33"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);

        // when & then
        assertThatThrownBy(() -> missionService.arrive(mission.getId(), robot.getMacAddress(), user.getId()))
                .isInstanceOf(MissionException.class)
                .extracting(exception -> ((MissionException) exception).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_MISSION_STATUS);
    }

    @Test
    @DisplayName("도착한 미션을 복귀 시작 처리하면 RETURNING 상태로 변경하고 MissionReturnStartedEvent를 발행한다")
    void returnStart() {
        // given
        User user = userRepository.save(User.createUser("return-user", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:41"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();
        mission.arrive();

        // when
        missionService.returnStart(mission.getId(), robot.getMacAddress(), user.getId());

        // then
        Mission returningMission = missionRepository.findById(mission.getId()).orElseThrow();

        assertThat(returningMission.getMissionStatus()).isEqualTo(MissionStatus.RETURNING);
        assertThat(events.stream(MissionReturnStartedEvent.class)).hasSize(1);
        assertThat(events.stream(MissionReturnStartedEvent.class).findFirst()).isPresent()
                .get()
                .extracting(
                        MissionReturnStartedEvent::missionId,
                        MissionReturnStartedEvent::robotId,
                        MissionReturnStartedEvent::userId,
                        MissionReturnStartedEvent::robotMacAddress
                )
                .containsExactly(mission.getId(), robot.getId(), user.getId(), robot.getMacAddress());
    }

    @Test
    @DisplayName("복귀 시작 처리 시 미션의 사용자나 로봇 mac address가 일치하지 않으면 MissionException을 던진다")
    void returnStartWithInvalidTarget() {
        // given
        User user = userRepository.save(User.createUser("return-user-2", "password"));
        User anotherUser = userRepository.save(User.createUser("return-user-3", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:42"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();
        mission.arrive();

        // when & then
        assertThatThrownBy(() -> missionService.returnStart(mission.getId(), "AA:BB:CC:DD:EE:99", anotherUser.getId()))
                .isInstanceOf(MissionException.class)
                .extracting(exception -> ((MissionException) exception).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_MISSION_STATUS);
    }

    @Test
    @DisplayName("미션 상태가 ARRIVED가 아니면 복귀 시작 처리 시 MissionException을 던진다")
    void returnStartWithInvalidMissionStatus() {
        // given
        User user = userRepository.save(User.createUser("return-user-4", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:43"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();

        // when & then
        assertThatThrownBy(() -> missionService.returnStart(mission.getId(), robot.getMacAddress(), user.getId()))
                .isInstanceOf(MissionException.class)
                .extracting(exception -> ((MissionException) exception).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_MISSION_STATUS);
    }

    @Test
    @DisplayName("복귀 중인 미션을 종료 처리하면 FINISHED 상태로 변경되고 로봇은 IDLE 상태가 된다")
    void finish() {
        // given
        User user = userRepository.save(User.createUser("finish-user", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:51"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();
        mission.arrive();
        mission.startReturning();

        // when
        missionService.finish(mission.getId(), robot.getMacAddress(), user.getId());

        // then
        Mission finishedMission = missionRepository.findById(mission.getId()).orElseThrow();

        assertThat(finishedMission.getMissionStatus()).isEqualTo(MissionStatus.FINISHED);
        assertThat(finishedMission.getRobot().getRobotStatus()).isEqualTo(com.e101.carry_porter.domain.robot.entity.RobotStatus.IDLE);
    }

    @Test
    @DisplayName("이미 FINISHED 상태인 미션에 종료 메시지가 다시 와도 예외 없이 그대로 유지한다")
    void finishWithAlreadyFinishedMissionStatus() {
        // given
        User user = userRepository.save(User.createUser("finish-user-duplicate", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:54"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();
        mission.arrive();
        mission.startReturning();
        mission.finish();

        // when
        missionService.finish(mission.getId(), robot.getMacAddress(), user.getId());

        // then
        Mission unchangedMission = missionRepository.findById(mission.getId()).orElseThrow();

        assertThat(unchangedMission.getMissionStatus()).isEqualTo(MissionStatus.FINISHED);
        assertThat(unchangedMission.getRobot().getRobotStatus()).isEqualTo(com.e101.carry_porter.domain.robot.entity.RobotStatus.IDLE);
    }

    @Test
    @DisplayName("이미 FAILED 상태인 미션에 종료 메시지가 와도 상태를 덮어쓰지 않는다")
    void finishWithFailedMissionStatus() {
        // given
        User user = userRepository.save(User.createUser("finish-user-failed", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:55"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();
        mission.fail();

        // when
        missionService.finish(mission.getId(), robot.getMacAddress(), user.getId());

        // then
        Mission unchangedMission = missionRepository.findById(mission.getId()).orElseThrow();

        assertThat(unchangedMission.getMissionStatus()).isEqualTo(MissionStatus.FAILED);
        assertThat(unchangedMission.getRobot().getRobotStatus()).isEqualTo(com.e101.carry_porter.domain.robot.entity.RobotStatus.BUSY);
    }

    @Test
    @DisplayName("종료 처리 시 미션의 사용자나 로봇 mac address가 일치하지 않으면 MissionException을 던진다")
    void finishWithInvalidTarget() {
        // given
        User user = userRepository.save(User.createUser("finish-user-2", "password"));
        User anotherUser = userRepository.save(User.createUser("finish-user-3", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:52"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();
        mission.arrive();
        mission.startReturning();

        // when & then
        assertThatThrownBy(() -> missionService.finish(mission.getId(), "AA:BB:CC:DD:EE:99", anotherUser.getId()))
                .isInstanceOf(MissionException.class)
                .extracting(exception -> ((MissionException) exception).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_MISSION_STATUS);
    }

    @Test
    @DisplayName("미션 상태가 RETURNING이 아니면 종료 처리 시 MissionException을 던진다")
    void finishWithInvalidMissionStatus() {
        // given
        User user = userRepository.save(User.createUser("finish-user-4", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:53"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();
        mission.arrive();

        // when & then
        assertThatThrownBy(() -> missionService.finish(mission.getId(), robot.getMacAddress(), user.getId()))
                .isInstanceOf(MissionException.class)
                .extracting(exception -> ((MissionException) exception).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_MISSION_STATUS);
    }

    @Test
    @DisplayName("진행 중인 미션을 실패 처리하면 FAILED 상태로 변경되고 로봇은 기존 BUSY 상태를 유지한다")
    void fail() {
        // given
        User user = userRepository.save(User.createUser("fail-user", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:61"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();

        // when
        missionService.fail(mission.getId(), robot.getMacAddress(), user.getId(), "ROBOT_EMERGENCY", "obstacle detected");

        // then
        Mission failedMission = missionRepository.findById(mission.getId()).orElseThrow();

        assertThat(failedMission.getMissionStatus()).isEqualTo(MissionStatus.FAILED);
        assertThat(failedMission.getRobot().getRobotStatus()).isEqualTo(com.e101.carry_porter.domain.robot.entity.RobotStatus.BUSY);
    }

    @Test
    @DisplayName("실패 처리 시 미션의 사용자나 로봇 mac address가 일치하지 않으면 MissionException을 던진다")
    void failWithInvalidTarget() {
        // given
        User user = userRepository.save(User.createUser("fail-user-2", "password"));
        User anotherUser = userRepository.save(User.createUser("fail-user-3", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:62"));
        Mission mission = missionRepository.save(Mission.createMission(user));
        mission.assignRobot(robot);
        mission.dispatch();

        // when & then
        assertThatThrownBy(() -> missionService.fail(
                mission.getId(),
                "AA:BB:CC:DD:EE:99",
                anotherUser.getId(),
                "ROBOT_EMERGENCY",
                "obstacle detected"
        ))
                .isInstanceOf(MissionException.class)
                .extracting(exception -> ((MissionException) exception).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_MISSION_STATUS);
    }

    @Test
    @DisplayName("이미 FINISHED 상태인 미션에 실패 메시지가 와도 상태를 덮어쓰지 않는다")
    void failWithFinishedMissionStatus() {
        // given
        User user = userRepository.save(User.createUser("fail-user-4", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:63"));
        Mission finishedMission = missionRepository.save(Mission.createMission(user));
        finishedMission.assignRobot(robot);
        finishedMission.dispatch();
        finishedMission.arrive();
        finishedMission.startReturning();
        finishedMission.finish();

        // when
        missionService.fail(
                finishedMission.getId(),
                robot.getMacAddress(),
                user.getId(),
                "ROBOT_EMERGENCY",
                "obstacle detected"
        );

        // then
        Mission unchangedMission = missionRepository.findById(finishedMission.getId()).orElseThrow();

        assertThat(unchangedMission.getMissionStatus()).isEqualTo(MissionStatus.FINISHED);
        assertThat(unchangedMission.getRobot().getRobotStatus()).isEqualTo(com.e101.carry_porter.domain.robot.entity.RobotStatus.IDLE);
    }

    @Test
    @DisplayName("이미 FAILED 상태인 미션에 실패 메시지가 다시 와도 예외 없이 그대로 유지한다")
    void failWithAlreadyFailedMissionStatus() {
        // given
        User user = userRepository.save(User.createUser("fail-user-5", "password"));
        Robot robot = robotRepository.save(Robot.createRobot("AA:BB:CC:DD:EE:64"));
        Mission failedMission = missionRepository.save(Mission.createMission(user));
        failedMission.assignRobot(robot);
        failedMission.dispatch();
        failedMission.fail();

        // when
        missionService.fail(
                failedMission.getId(),
                robot.getMacAddress(),
                user.getId(),
                "ROBOT_EMERGENCY",
                "obstacle detected"
        );

        // then
        Mission unchangedMission = missionRepository.findById(failedMission.getId()).orElseThrow();

        assertThat(unchangedMission.getMissionStatus()).isEqualTo(MissionStatus.FAILED);
        assertThat(unchangedMission.getRobot().getRobotStatus()).isEqualTo(com.e101.carry_porter.domain.robot.entity.RobotStatus.BUSY);
    }
}
