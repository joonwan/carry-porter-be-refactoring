package com.e101.carry_porter.domain.mission.service;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.mission.event.MissionCreatedEvent;
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
}
