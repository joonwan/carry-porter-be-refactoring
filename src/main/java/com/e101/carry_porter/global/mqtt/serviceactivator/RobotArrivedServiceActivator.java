package com.e101.carry_porter.global.mqtt.serviceactivator;

import com.e101.carry_porter.domain.mission.event.MissionArrivedEvent;
import com.e101.carry_porter.domain.robot.mqtt.RobotInboundMessage;
import com.e101.carry_porter.domain.robot.mqtt.RobotInboundPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carry-porter.mqtt", name = "enabled", havingValue = "true")
public class RobotArrivedServiceActivator {

    private final ApplicationEventPublisher eventPublisher;

    @ServiceActivator(inputChannel = "robotArrivedChannel")
    public void handle(Message<RobotInboundMessage> message) {
        RobotInboundMessage inboundMessage = message.getPayload();
        RobotInboundPayload payload = inboundMessage.payload();

        eventPublisher.publishEvent(new MissionArrivedEvent(
                payload.missionId(),
                inboundMessage.macAddress(),
                payload.userId()
        ));

        log.info("MissionArrivedEvent 발행: missionId = {}, robotMacAddress = {}, userId = {}",
                payload.missionId(), inboundMessage.macAddress(), payload.userId());
    }
}
