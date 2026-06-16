package com.e101.carry_porter.domain.robot.listener;

import com.e101.carry_porter.domain.mission.event.MissionStartedEvent;
import com.e101.carry_porter.domain.robot.service.MqttCommandPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotCommandEventListener {

    private final MqttCommandPublisher mqttCommandPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionStartedEvent(MissionStartedEvent event) {
        log.info("MissionStartedEvent 수신: mission id = {}. robot id = {}",
                event.missionId(),
                event.robotId());

        mqttCommandPublisher.publishDeparture(
                event.missionId(),
                event.userId(),
                event.robotMacAddress());
    }
}
