package com.e101.carry_porter.domain.notification.listener;

import com.e101.carry_porter.domain.mission.event.MissionArrivedEvent;
import com.e101.carry_porter.domain.mission.event.MissionFailedEvent;
import com.e101.carry_porter.domain.mission.event.MissionFinishedEvent;
import com.e101.carry_porter.domain.mission.event.MissionReturnStartedEvent;
import com.e101.carry_porter.domain.mission.event.MissionStartedEvent;
import com.e101.carry_porter.domain.notification.dto.NotificationPayload;
import com.e101.carry_porter.domain.notification.service.NotificationService;
import com.e101.carry_porter.domain.robot.event.RobotAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRobotAssignedEvent(RobotAssignedEvent event) {
        log.info("RobotAssignedEvent 수신: missionId = {}, userId = {}",
                event.missionId(), event.userId());

        notificationService.createNotification(
                NotificationPayload.of(
                        "ROBOT_ASSIGNED",
                        event.missionId(),
                        event.userId(),
                        "로봇 배정이 완료되었습니다."
                )
        );
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionStartedEvent(MissionStartedEvent event) {
        log.info("MissionStartedEvent 수신: missionId = {}, userId = {}",
                event.missionId(), event.userId());

        notificationService.createNotification(
                NotificationPayload.of(
                        "MISSION_STARTED",
                        event.missionId(),
                        event.userId(),
                        "로봇이 출발했습니다."
                )
        );
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionArrivedEvent(MissionArrivedEvent event) {
        log.info("MissionArrivedEvent 수신: missionId = {}, userId = {}",
                event.missionId(), event.userId());

        notificationService.createNotification(
                NotificationPayload.of(
                        "MISSION_ARRIVED",
                        event.missionId(),
                        event.userId(),
                        "로봇이 목적지에 도착했습니다."
                )
        );
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionReturnStartedEvent(MissionReturnStartedEvent event) {
        log.info("MissionReturnStartedEvent 수신: missionId = {}, userId = {}",
                event.missionId(), event.userId());

        notificationService.createNotification(
                NotificationPayload.of(
                        "MISSION_RETURN_STARTED",
                        event.missionId(),
                        event.userId(),
                        "로봇이 복귀를 시작했습니다."
                )
        );
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionFinishedEvent(MissionFinishedEvent event) {
        log.info("MissionFinishedEvent 수신: missionId = {}, userId = {}",
                event.missionId(), event.userId());

        notificationService.createNotification(
                NotificationPayload.of(
                        "MISSION_FINISHED",
                        event.missionId(),
                        event.userId(),
                        "미션이 완료되었습니다."
                )
        );
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionFailedEvent(MissionFailedEvent event) {
        log.info("MissionFailedEvent 수신: missionId = {}, userId = {}, failureCode = {}",
                event.missionId(), event.userId(), event.failureCode());

        notificationService.createNotification(
                NotificationPayload.failure(
                        event.missionId(),
                        event.userId(),
                        "미션 수행 중 오류가 발생했습니다.",
                        event.failureCode()
                )
        );
    }
}
