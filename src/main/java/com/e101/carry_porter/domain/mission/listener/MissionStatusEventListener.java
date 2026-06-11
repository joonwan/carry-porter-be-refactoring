package com.e101.carry_porter.domain.mission.listener;

import com.e101.carry_porter.domain.mission.service.MissionService;
import com.e101.carry_porter.domain.robot.event.RobotAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
