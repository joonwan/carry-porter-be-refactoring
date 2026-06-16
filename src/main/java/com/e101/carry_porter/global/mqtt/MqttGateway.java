package com.e101.carry_porter.global.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttGateway {

    @Qualifier("mqttOutboundChannel")
    private final MessageChannel mqttOutboundChannel;

    public void publish(String topic, String payload) {

        Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .build();

        log.info("message 발행: topic = {}", topic);
        mqttOutboundChannel.send(message);
    }
}
