package com.e101.carry_porter.domain.mission.listener;

import com.e101.carry_porter.domain.mission.event.MissionArrivedEvent;
import com.e101.carry_porter.domain.mission.event.MissionFailedEvent;
import com.e101.carry_porter.domain.mission.event.MissionFinishedEvent;
import com.e101.carry_porter.domain.mission.service.MissionService;
import com.e101.carry_porter.domain.robot.event.RobotAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionStatusEventListener {

    private final MissionService missionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRobotAssignedEvent(RobotAssignedEvent event) {
        log.info("RobotAssignedEvent 수신: missionId = {}, robotId = {}, userId = {}",
                event.missionId(), event.robotId(), event.userId());
        missionService.dispatch(event.missionId(), event.robotId(), event.userId());
    }

    @EventListener
    public void handleMissionArrivedEvent(MissionArrivedEvent event) {
        log.info("MissionArrivedEvent 수신: missionId = {}, robotMacAddress = {}, userId = {}",
                event.missionId(), event.robotMacAddress(), event.userId());
        missionService.arrive(event.missionId(), event.robotMacAddress(), event.userId());
    }

    @EventListener
    public void handleMissionFinishedEvent(MissionFinishedEvent event) {
        log.info("MissionFinishedEvent 수신: missionId = {}, robotMacAddress = {}, userId = {}",
                event.missionId(), event.robotMacAddress(), event.userId());
        missionService.finish(event.missionId(), event.robotMacAddress(), event.userId());
    }

    @EventListener
    public void handleMissionFailedEvent(MissionFailedEvent event) {
        log.info("MissionFailedEvent 수신: missionId = {}, robotMacAddress = {}, userId = {}, failureCode = {}",
                event.missionId(), event.robotMacAddress(), event.userId(), event.failureCode());
        missionService.fail(
                event.missionId(),
                event.robotMacAddress(),
                event.userId(),
                event.failureCode(),
                event.message()
        );
    }
}
