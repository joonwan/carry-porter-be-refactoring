package com.e101.carry_porter.domain.robot.service;

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

    private final RobotRepository robotRepository;
    private final MissionRepository missionRepository;
    private final ApplicationEventPublisher eventPublisher;

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
                .orElseThrow(() -> new RobotException(RobotErrorCode.AVAILABLE_ROBOT_NOT_FOUND));

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
}
