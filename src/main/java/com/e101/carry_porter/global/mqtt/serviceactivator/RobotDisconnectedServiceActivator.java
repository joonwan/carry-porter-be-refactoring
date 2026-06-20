package com.e101.carry_porter.global.mqtt.serviceactivator;

import com.e101.carry_porter.domain.robot.event.RobotDisconnectedMessageReceivedEvent;
import com.e101.carry_porter.domain.robot.mqtt.RobotInboundMessage;
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
public class RobotDisconnectedServiceActivator {

    private final ApplicationEventPublisher eventPublisher;

    @ServiceActivator(inputChannel = "robotDisconnectedChannel")
    public void handle(Message<RobotInboundMessage> message) {
        RobotInboundMessage inboundMessage = message.getPayload();

        log.info("유언장 발행 mac address = {}", inboundMessage.macAddress());

        eventPublisher.publishEvent(new RobotDisconnectedMessageReceivedEvent(
                inboundMessage.payload().robotEventId(),
                inboundMessage.macAddress()
        ));

        log.info("RobotDisconnectedMessageReceivedEvent 발행: robotEventId = {}, robotMacAddress = {}",
                inboundMessage.payload().robotEventId(), inboundMessage.macAddress());
    }
}
