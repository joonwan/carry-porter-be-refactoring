package com.e101.carry_porter.domain.mission.service;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.mission.entity.MissionStatus;
import com.e101.carry_porter.domain.mission.event.MissionArrivedEvent;
import com.e101.carry_porter.domain.mission.event.MissionCreatedEvent;
import com.e101.carry_porter.domain.mission.event.MissionFailedEvent;
import com.e101.carry_porter.domain.mission.event.MissionFinishedEvent;
import com.e101.carry_porter.domain.mission.event.MissionReturnStartedEvent;
import com.e101.carry_porter.domain.mission.event.MissionStartedEvent;
import com.e101.carry_porter.domain.mission.exception.MissionErrorCode;
import com.e101.carry_porter.domain.mission.exception.MissionException;
import com.e101.carry_porter.domain.mission.repository.MissionRepository;
import com.e101.carry_porter.domain.mission.service.dto.request.CreateMissionServiceRequest;
import com.e101.carry_porter.domain.mission.service.dto.response.CreateMissionServiceResponse;
import com.e101.carry_porter.domain.robot.service.RobotEventDedupService;
import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.domain.user.exception.UserErrorCode;
import com.e101.carry_porter.domain.user.exception.UserException;
import com.e101.carry_porter.domain.user.repository.UserRepository;
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
public class MissionService {

    private static final List<MissionStatus> ACTIVE_MISSION_STATUSES = List.of(
            MissionStatus.CREATED,
            MissionStatus.ASSIGNED,
            MissionStatus.DISPATCHED,
            MissionStatus.ARRIVED,
            MissionStatus.RETURNING
    );

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final RobotEventDedupService robotEventDedupService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CreateMissionServiceResponse createMission(CreateMissionServiceRequest request) {

        log.info("mission 생성 요청: userId = {}", request.userId());
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        validateNoActiveMission(user.getId());

        Mission mission = Mission.createMission(user);
        Mission savedMission = missionRepository.save(mission);
        log.info("mission 생성 완료: userId = {}, missionId = {}", request.userId(), savedMission.getId());

        eventPublisher.publishEvent(new MissionCreatedEvent(savedMission.getId(), user.getId()));
        log.info("MissionCreatedEvent 발행 완료: missionId = {}", savedMission.getId());
        return CreateMissionServiceResponse.from(savedMission);
    }

    @Transactional
    public void dispatch(Long missionId, Long robotId, Long userId) {
        log.info("mission 시작 처리: missionId = {}, robotId = {}, userId = {}",
                missionId, robotId, userId);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        // mission 의 로봇 그리고 사용자가 각각 일치하는지 검증
        validateDispatchTarget(mission, robotId, userId);

        // mission 상태가 ASSIGNED 인지 검증
        validateDispatchStatus(mission);

        mission.dispatch();

        eventPublisher.publishEvent(new MissionStartedEvent(
                missionId,
                robotId,
                userId,
                mission.getRobot().getMacAddress()
        ));
        log.info("MissionStartedEvent 발행 완료: missionId = {}, robotId = {}, userId = {}",
                missionId, robotId, userId);
    }

    @Transactional
    public void arrive(Long missionId, String robotEventId, String robotMacAddress, Long userId) {
        log.info("mission 도착 처리: missionId = {}, robotMacAddress = {}, userId = {}",
                missionId, robotMacAddress, userId);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        validateArrivalTarget(mission, robotMacAddress, userId);

        if (mission.getMissionStatus() == MissionStatus.ARRIVED
                || mission.getMissionStatus() == MissionStatus.RETURNING
                || mission.getMissionStatus() == MissionStatus.FINISHED
                || mission.getMissionStatus() == MissionStatus.FAILED) {
            robotEventDedupService.markProcessedRobotEvent(robotEventId, robotMacAddress);
            log.info("이미 도착 처리되었거나 이후 단계로 진행된 mission 이므로 도착 처리를 건너뜁니다: missionId = {}, currentStatus = {}",
                    missionId, mission.getMissionStatus());
            return;
        }

        validateArrivalStatus(mission);

        mission.arrive();
        robotEventDedupService.markProcessedRobotEvent(robotEventId, robotMacAddress);

        eventPublisher.publishEvent(new MissionArrivedEvent(
                missionId,
                robotMacAddress,
                userId
        ));

        log.info("MissionArrivedEvent 발행 완료: missionId = {}, robotMacAddress = {}, userId = {}",
                missionId, robotMacAddress, userId);
        log.info("mission 도착 처리 완료: missionId = {}, robotMacAddress = {}, userId = {}, mission status = {}",
                missionId, robotMacAddress, userId, mission.getMissionStatus());
    }

    @Transactional
    public void returnStart(Long missionId, String robotMacAddress, Long userId) {
        log.info("mission 복귀 시작 처리: missionId = {}, robotMacAddress = {}, userId = {}",
                missionId, robotMacAddress, userId);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        validateArrivalTarget(mission, robotMacAddress, userId);
        validateReturnStartStatus(mission);

        mission.startReturning();

        eventPublisher.publishEvent(new MissionReturnStartedEvent(
                missionId,
                mission.getRobot().getId(),
                userId,
                robotMacAddress
        ));

        log.info("MissionReturnStartedEvent 발행 완료: missionId = {}, robotId = {}, userId = {}",
                missionId, mission.getRobot().getId(), userId);
    }

    @Transactional
    public void returnStart(Long missionId, Long userId) {
        log.info("mission 복귀 시작 요청: missionId = {}, userId = {}", missionId, userId);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        if (!mission.getUser().getId().equals(userId) || mission.getRobot() == null) {
            throw new MissionException(MissionErrorCode.INVALID_MISSION_STATUS);
        }

        returnStart(missionId, mission.getRobot().getMacAddress(), userId);
    }

    @Transactional
    public void finish(Long missionId, String robotEventId, String robotMacAddress, Long userId) {
        log.info("mission 종료 처리: missionId = {}, robotMacAddress = {}, userId = {}",
                missionId, robotMacAddress, userId);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        validateArrivalTarget(mission, robotMacAddress, userId);

        if (mission.getMissionStatus() == MissionStatus.FINISHED) {
            robotEventDedupService.markProcessedRobotEvent(robotEventId, robotMacAddress);
            log.info("이미 종료 처리된 mission 이므로 중복 종료 처리를 건너뜁니다: missionId = {}", missionId);
            return;
        }

        if (mission.getMissionStatus() == MissionStatus.FAILED) {
            robotEventDedupService.markProcessedRobotEvent(robotEventId, robotMacAddress);
            log.warn("이미 실패 처리된 mission 이므로 종료 처리를 건너뜁니다: missionId = {}", missionId);
            return;
        }

        validateFinishStatus(mission);

        mission.finish();
        robotEventDedupService.markProcessedRobotEvent(robotEventId, robotMacAddress);

        eventPublisher.publishEvent(new MissionFinishedEvent(
                missionId,
                robotMacAddress,
                userId
        ));

        log.info("MissionFinishedEvent 발행 완료: missionId = {}, robotMacAddress = {}, userId = {}",
                missionId, robotMacAddress, userId);
        log.info("mission 종료 처리 완료: missionId = {}, robotMacAddress = {}, userId = {}, mission status = {}",
                missionId, robotMacAddress, userId, mission.getMissionStatus());
    }

    @Transactional
    public void fail(Long missionId, String robotEventId, String robotMacAddress, Long userId, String failureCode, String message) {
        log.info("mission 실패 처리: missionId = {}, robotMacAddress = {}, userId = {}, failureCode = {}, message = {}",
                missionId, robotMacAddress, userId, failureCode, message);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        validateArrivalTarget(mission, robotMacAddress, userId);

        if (mission.getMissionStatus() == MissionStatus.FINISHED) {
            robotEventDedupService.markProcessedRobotEvent(robotEventId, robotMacAddress);
            log.warn("이미 종료된 mission 이므로 실패 처리를 건너뜁니다: missionId = {}", missionId);
            return;
        }

        if (mission.getMissionStatus() == MissionStatus.FAILED) {
            robotEventDedupService.markProcessedRobotEvent(robotEventId, robotMacAddress);
            log.info("이미 실패 처리된 mission 이므로 중복 실패 처리를 건너뜁니다: missionId = {}", missionId);
            return;
        }

        mission.fail();
        robotEventDedupService.markProcessedRobotEvent(robotEventId, robotMacAddress);

        eventPublisher.publishEvent(new MissionFailedEvent(
                missionId,
                robotMacAddress,
                userId,
                failureCode,
                message
        ));

        log.info("MissionFailedEvent 발행 완료: missionId = {}, robotMacAddress = {}, userId = {}, failureCode = {}",
                missionId, robotMacAddress, userId, failureCode);
        log.info("mission 실패 처리 완료: missionId = {}, robotMacAddress = {}, userId = {}, failureCode = {}, mission status = {}",
                missionId, robotMacAddress, userId, failureCode, mission.getMissionStatus());
    }

    @Transactional
    public void failAssignment(Long missionId, Long userId, String failureCode, String message) {
        log.info("mission 배정 실패 처리: missionId = {}, userId = {}, failureCode = {}, message = {}",
                missionId, userId, failureCode, message);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        if (!mission.getUser().getId().equals(userId)) {
            throw new MissionException(MissionErrorCode.INVALID_MISSION_STATUS);
        }

        if (mission.getMissionStatus() == MissionStatus.FAILED) {
            log.info("이미 실패 처리된 mission 이므로 배정 실패 처리를 건너뜁니다: missionId = {}", missionId);
            return;
        }

        if (mission.getMissionStatus() != MissionStatus.CREATED) {
            throw new MissionException(MissionErrorCode.INVALID_MISSION_STATUS);
        }

        mission.fail();

        eventPublisher.publishEvent(new MissionFailedEvent(
                missionId,
                null,
                userId,
                failureCode,
                message
        ));

        log.info("MissionFailedEvent 발행 완료: missionId = {}, userId = {}, failureCode = {}",
                missionId, userId, failureCode);
    }

    private void validateDispatchTarget(Mission mission, Long robotId, Long userId) {
        if (!mission.getUser().getId().equals(userId)) {
            throw new MissionException(MissionErrorCode.INVALID_MISSION_STATUS);
        }

        if (mission.getRobot() == null || !mission.getRobot().getId().equals(robotId)) {
            throw new MissionException(MissionErrorCode.INVALID_MISSION_STATUS);
        }
    }

    private void validateDispatchStatus(Mission mission) {
        if (mission.getMissionStatus() != MissionStatus.ASSIGNED) {
            throw new MissionException(MissionErrorCode.INVALID_MISSION_STATUS);
        }
    }

    private void validateArrivalTarget(Mission mission, String robotMacAddress, Long userId) {
        if (!mission.getUser().getId().equals(userId)) {
            throw new MissionException(MissionErrorCode.INVALID_MISSION_STATUS);
        }

        if (mission.getRobot() == null || !mission.getRobot().getMacAddress().equals(robotMacAddress)) {
            throw new MissionException(MissionErrorCode.INVALID_MISSION_STATUS);
        }
    }

    private void validateArrivalStatus(Mission mission) {
        if (mission.getMissionStatus() != MissionStatus.DISPATCHED) {
            throw new MissionException(MissionErrorCode.INVALID_MISSION_STATUS);
        }
    }

    private void validateReturnStartStatus(Mission mission) {
        if (mission.getMissionStatus() != MissionStatus.ARRIVED) {
            throw new MissionException(MissionErrorCode.INVALID_MISSION_STATUS);
        }
    }

    private void validateFinishStatus(Mission mission) {
        if (mission.getMissionStatus() != MissionStatus.RETURNING) {
            throw new MissionException(MissionErrorCode.INVALID_MISSION_STATUS);
        }
    }

    private void validateNoActiveMission(Long userId) {
        if (missionRepository.existsByUserIdAndMissionStatusIn(userId, ACTIVE_MISSION_STATUSES)) {
            throw new MissionException(MissionErrorCode.MISSION_ALREADY_IN_PROGRESS);
        }
    }

}
