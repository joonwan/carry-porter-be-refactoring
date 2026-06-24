package com.e101.carry_porter.domain.mission.listener;

import com.e101.carry_porter.domain.mission.service.MissionService;
import com.e101.carry_porter.domain.robot.event.RobotAssignedEvent;
import com.e101.carry_porter.domain.robot.event.RobotAssignmentFailedEvent;
import com.e101.carry_porter.domain.robot.event.RobotArrivedMessageReceivedEvent;
import com.e101.carry_porter.domain.robot.event.RobotEmergencyMessageReceivedEvent;
import com.e101.carry_porter.domain.robot.event.RobotReturnedMessageReceivedEvent;
import com.e101.carry_porter.domain.robot.exception.RobotErrorCode;
import com.e101.carry_porter.domain.robot.exception.RobotException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionStatusEventListener {

    private final MissionService missionService;

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRobotAssignedEvent(RobotAssignedEvent event) {
        log.info("RobotAssignedEvent 수신: missionId = {}, robotId = {}, userId = {}",
                event.missionId(), event.robotId(), event.userId());
        missionService.dispatch(event.missionId(), event.robotId(), event.userId());
    }

    @Async("eventTaskExecutor")
    @EventListener
    public void handleRobotAssignmentFailedEvent(RobotAssignmentFailedEvent event) {
        log.info("RobotAssignmentFailedEvent 수신: missionId = {}, userId = {}, failureCode = {}",
                event.missionId(), event.userId(), event.failureCode());
        missionService.failAssignment(
                event.missionId(),
                event.userId(),
                event.failureCode(),
                event.message()
        );
    }

    @Async("eventTaskExecutor")
    @EventListener
    public void handleRobotArrivedMessageReceivedEvent(RobotArrivedMessageReceivedEvent event) {
        log.info("RobotArrivedMessageReceivedEvent 수신: missionId = {}, robotEventId = {}, robotMacAddress = {}, userId = {}",
                event.missionId(), event.robotEventId(), event.robotMacAddress(), event.userId());
        try {
            missionService.arrive(event.missionId(), event.robotEventId(), event.robotMacAddress(), event.userId());
        } catch (RobotException exception) {
            handleDuplicateRobotEvent(exception, event.robotEventId(), event.robotMacAddress());
        }
    }

    @Async("eventTaskExecutor")
    @EventListener
    public void handleRobotReturnedMessageReceivedEvent(RobotReturnedMessageReceivedEvent event) {
        log.info("RobotReturnedMessageReceivedEvent 수신: missionId = {}, robotEventId = {}, robotMacAddress = {}, userId = {}",
                event.missionId(), event.robotEventId(), event.robotMacAddress(), event.userId());
        try {
            missionService.finish(event.missionId(), event.robotEventId(), event.robotMacAddress(), event.userId());
        } catch (RobotException exception) {
            handleDuplicateRobotEvent(exception, event.robotEventId(), event.robotMacAddress());
        }
    }

    @Async("eventTaskExecutor")
    @EventListener
    public void handleRobotEmergencyMessageReceivedEvent(RobotEmergencyMessageReceivedEvent event) {
        log.info("RobotEmergencyMessageReceivedEvent 수신: missionId = {}, robotEventId = {}, robotMacAddress = {}, userId = {}, failureCode = {}",
                event.missionId(), event.robotEventId(), event.robotMacAddress(), event.userId(), event.failureCode());
        try {
            missionService.fail(
                    event.missionId(),
                    event.robotEventId(),
                    event.robotMacAddress(),
                    event.userId(),
                    event.failureCode(),
                    event.message()
            );
        } catch (RobotException exception) {
            handleDuplicateRobotEvent(exception, event.robotEventId(), event.robotMacAddress());
        }
    }

    private void handleDuplicateRobotEvent(RobotException exception, String robotEventId, String robotMacAddress) {
        if (exception.getErrorCode() != RobotErrorCode.DUPLICATE_ROBOT_EVENT) {
            throw exception;
        }

        log.info("이미 처리된 robot 이벤트이므로 mission 상태 처리를 건너뜁니다: robotEventId = {}, robotMacAddress = {}",
                robotEventId, robotMacAddress);
    }
}
