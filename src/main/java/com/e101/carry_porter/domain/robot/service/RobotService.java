package com.e101.carry_porter.domain.robot.service;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.mission.entity.MissionStatus;
import com.e101.carry_porter.domain.mission.event.MissionFailedEvent;
import com.e101.carry_porter.domain.mission.exception.MissionErrorCode;
import com.e101.carry_porter.domain.mission.exception.MissionException;
import com.e101.carry_porter.domain.mission.repository.MissionRepository;
import com.e101.carry_porter.domain.robot.entity.Robot;
import com.e101.carry_porter.domain.robot.entity.RobotStatus;
import com.e101.carry_porter.domain.robot.event.RobotAssignedEvent;
import com.e101.carry_porter.domain.robot.event.RobotAssignmentFailedEvent;
import com.e101.carry_porter.domain.robot.exception.RobotErrorCode;
import com.e101.carry_porter.domain.robot.exception.RobotException;
import com.e101.carry_porter.domain.robot.repository.RobotRepository;
import com.e101.carry_porter.domain.robot.service.dto.request.AssignRobotServiceRequest;
import com.e101.carry_porter.domain.robot.service.dto.response.AssignRobotServiceResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RobotService {

    private static final List<MissionStatus> ROBOT_ACTIVE_MISSION_STATUSES = List.of(
            MissionStatus.ASSIGNED,
            MissionStatus.DISPATCHED,
            MissionStatus.ARRIVED,
            MissionStatus.RETURNING
    );

    private final RobotRepository robotRepository;
    private final MissionRepository missionRepository;
    private final RobotEventDedupService robotEventDedupService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void registerOrReconnect(String robotEventId, String macAddress) {
        log.info("robot 등록 또는 재연결 처리 시작: macAddress = {}", macAddress);

        Robot robot = robotRepository.findByMacAddress(macAddress)
                .map(this::synchronizeRobotStatus)
                .orElseGet(() -> createConnectedRobot(macAddress));

        robotEventDedupService.markProcessedRobotEvent(robotEventId, macAddress);

        log.info("robot 등록 또는 재연결 처리 완료: robotId = {}, macAddress = {}, robotStatus = {}",
                robot.getId(), robot.getMacAddress(), robot.getRobotStatus());
    }

    @Transactional
    public void disconnect(String robotEventId, String macAddress) {
        log.info("robot 연결 끊김 처리 시작: macAddress = {}", macAddress);

        Robot robot = robotRepository.findByMacAddress(macAddress)
                .orElseThrow(() -> new RobotException(RobotErrorCode.ROBOT_NOT_FOUND));

        RobotStatus previousStatus = robot.getRobotStatus();
        robot.toOffline();
        robotEventDedupService.markProcessedRobotEvent(robotEventId, macAddress);
        failActiveMissionByDisconnectedRobot(robot);

        log.info("robot 상태가 OFFLINE 으로 변경되었습니다: robotId = {}, macAddress = {}, previousStatus = {}, currentStatus = {}",
                robot.getId(), robot.getMacAddress(), previousStatus, robot.getRobotStatus());
    }

    @Transactional
    public AssignRobotServiceResponse assignRobot(AssignRobotServiceRequest request) {

        log.info("robot 배정 시작: missionId = {}", request.missionId());
        log.info("데이터 저장되어 있는지 확인");
        log.info("mission count = {}, robot count = {}", missionRepository.findAll().size(), robotRepository.findAll().size());

        // mission 조회
        Mission mission = missionRepository.findById(request.missionId())
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        // mission 이 CREATED 상태인지 검증
        validateMissionStatus(mission);

        // 가용 로봇 조회 (동시성 제어)
        Robot robot = robotRepository.findFirstByRobotStatusOrderByIdAsc(RobotStatus.IDLE)
                .orElse(null);

        if (robot == null) {
            publishRobotAssignmentFailedEvent(mission, RobotErrorCode.AVAILABLE_ROBOT_NOT_FOUND);
            throw new RobotException(RobotErrorCode.AVAILABLE_ROBOT_NOT_FOUND);
        }

        // mission 에 로봇 할당 + robot 상태를 BUSY 로 변경
        mission.assignRobot(robot);

        // robot 할당 완료 이벤트 발행
        eventPublisher.publishEvent(new RobotAssignedEvent(
                mission.getId(),
                robot.getId(),
                mission.getUser().getId()
        ));

        log.info("로봇 배정 완료: missionId = {}, robotId = {}, userId = {}",
                mission.getId(), robot.getId(), mission.getUser().getId());

        return AssignRobotServiceResponse.from(mission, robot);
    }

    private void validateMissionStatus(Mission mission) {
        if (mission.getMissionStatus() != MissionStatus.CREATED) {
            throw new MissionException(MissionErrorCode.INVALID_MISSION_STATUS);
        }
    }

    private void publishRobotAssignmentFailedEvent(Mission mission, RobotErrorCode errorCode) {
        eventPublisher.publishEvent(new RobotAssignmentFailedEvent(
                mission.getId(),
                mission.getUser().getId(),
                errorCode.getCode(),
                errorCode.getMessage()
        ));

        log.warn("RobotAssignmentFailedEvent 발행: missionId = {}, userId = {}, failureCode = {}",
                mission.getId(), mission.getUser().getId(), errorCode.getCode());
    }

    private void failActiveMissionByDisconnectedRobot(Robot robot) {
        missionRepository.findFirstByRobotIdAndMissionStatusInOrderByIdDesc(
                        robot.getId(),
                        ROBOT_ACTIVE_MISSION_STATUSES
                )
                .ifPresent(mission -> {
                    mission.fail();

                    eventPublisher.publishEvent(new MissionFailedEvent(
                            mission.getId(),
                            robot.getMacAddress(),
                            mission.getUser().getId(),
                            RobotErrorCode.ROBOT_DISCONNECTED.getCode(),
                            RobotErrorCode.ROBOT_DISCONNECTED.getMessage()
                    ));

                    log.warn("로봇 연결 끊김으로 미션 실패 처리: missionId = {}, robotId = {}, userId = {}, failureCode = {}",
                            mission.getId(), robot.getId(), mission.getUser().getId(),
                            RobotErrorCode.ROBOT_DISCONNECTED.getCode());
                });
    }

    private Robot synchronizeRobotStatus(Robot robot) {
        if (robot.getRobotStatus() == RobotStatus.OFFLINE) {
            robot.toIdle();
        }

        return robot;
    }

    private Robot createConnectedRobot(String macAddress) {
        Robot robot = Robot.createRobot(macAddress);
        return robotRepository.save(robot);
    }
}
