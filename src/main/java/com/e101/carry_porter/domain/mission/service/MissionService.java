package com.e101.carry_porter.domain.mission.service;

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
import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.domain.user.exception.UserErrorCode;
import com.e101.carry_porter.domain.user.exception.UserException;
import com.e101.carry_porter.domain.user.repository.UserRepository;
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

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CreateMissionServiceResponse createMission(CreateMissionServiceRequest request) {

        log.info("mission 생성 요청: userId = {}", request.userId());
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

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
    public void arrive(Long missionId, String robotMacAddress, Long userId) {
        log.info("mission 도착 처리: missionId = {}, robotMacAddress = {}, userId = {}",
                missionId, robotMacAddress, userId);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        validateArrivalTarget(mission, robotMacAddress, userId);
        validateArrivalStatus(mission);

        mission.arrive();

        log.info("mission 도착 처리 완료: missionId = {}, robotMacAddress = {}, userId = {}",
                missionId, robotMacAddress, userId);
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
    public void finish(Long missionId, String robotMacAddress, Long userId) {
        log.info("mission 종료 처리: missionId = {}, robotMacAddress = {}, userId = {}",
                missionId, robotMacAddress, userId);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        validateArrivalTarget(mission, robotMacAddress, userId);
        validateFinishStatus(mission);

        mission.finish();

        log.info("mission 종료 처리 완료: missionId = {}, robotMacAddress = {}, userId = {}",
                missionId, robotMacAddress, userId);
    }

    @Transactional
    public void fail(Long missionId, String robotMacAddress, Long userId, String failureCode, String message) {
        log.info("mission 실패 처리: missionId = {}, robotMacAddress = {}, userId = {}, failureCode = {}, message = {}",
                missionId, robotMacAddress, userId, failureCode, message);

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        validateArrivalTarget(mission, robotMacAddress, userId);

        if (mission.getMissionStatus() == MissionStatus.FINISHED) {
            log.warn("이미 종료된 mission 이므로 실패 처리를 건너뜁니다: missionId = {}", missionId);
            return;
        }

        if (mission.getMissionStatus() == MissionStatus.FAILED) {
            log.info("이미 실패 처리된 mission 이므로 중복 실패 처리를 건너뜁니다: missionId = {}", missionId);
            return;
        }

        mission.fail();

        log.info("mission 실패 처리 완료: missionId = {}, robotMacAddress = {}, userId = {}, failureCode = {}",
                missionId, robotMacAddress, userId, failureCode);
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

}
