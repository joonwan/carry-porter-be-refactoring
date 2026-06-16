package com.e101.carry_porter.global.config.mqtt;

import com.e101.carry_porter.domain.robot.mqtt.RobotInboundMessage;
import com.e101.carry_porter.domain.robot.mqtt.RobotInboundPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carry-porter.mqtt", name = "enabled", havingValue = "true")
public class RobotMqttInboundFlowConfig {

    private static final String EVENT_ARRIVED = "arrived";
    private static final String EVENT_RETURNED = "returned";
    private static final String EVENT_EMERGENCY = "emergency";

    private final ObjectMapper objectMapper;

    @Transformer(inputChannel = "mqttInboundChannel", outputChannel = "robotEventRouterChannel")
    public RobotInboundMessage robotInboundTransformer(Message<String> message) {
        String topic = String.valueOf(message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
        String[] topicTokens = topic.split("/");
        String macAddress = topicTokens[2];
        String eventName = topicTokens[4];
        String payload = message.getPayload();

        log.info("MQTT inbound message 수신: topic = {}, payload = {}", topic, payload);

        try {
            RobotInboundPayload inboundPayload = objectMapper.readValue(payload, RobotInboundPayload.class);
            return new RobotInboundMessage(macAddress, eventName, inboundPayload);
        } catch (Exception exception) {
            throw new IllegalArgumentException("MQTT inbound payload 역직렬화에 실패했습니다.", exception);
        }
    }

    @Bean
    @Router(inputChannel = "robotEventRouterChannel")
    public AbstractMessageRouter robotEventRouter(
            MessageChannel robotArrivedChannel,
            MessageChannel robotReturnedChannel,
            MessageChannel robotEmergencyChannel
    ) {
        return new AbstractMessageRouter() {
            @Override
            protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
                RobotInboundMessage inboundMessage = (RobotInboundMessage) message.getPayload();

                return switch (inboundMessage.eventName()) {
                    case EVENT_ARRIVED -> List.of(robotArrivedChannel);
                    case EVENT_RETURNED -> List.of(robotReturnedChannel);
                    case EVENT_EMERGENCY -> List.of(robotEmergencyChannel);
                    default -> List.of();
                };
            }
        };
    }
}
