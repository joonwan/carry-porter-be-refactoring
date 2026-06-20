package com.e101.carry_porter.global.config.mqtt;

import com.e101.carry_porter.domain.robot.mqtt.RobotInboundMessage;
import com.e101.carry_porter.domain.robot.mqtt.RobotInboundPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import com.e101.carry_porter.domain.robot.service.RobotEventDedupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carry-porter.mqtt", name = "enabled", havingValue = "true")
public class RobotMqttInboundFlowConfig {

    private static final String EVENT_CONNECTED = "connected";
    private static final String EVENT_DISCONNECTED = "disconnected";
    private static final String EVENT_ARRIVED = "arrived";
    private static final String EVENT_RETURNED = "returned";
    private static final String EVENT_EMERGENCY = "emergency";

    private final ObjectMapper objectMapper;
    private final RobotEventDedupService robotEventDedupService;

    @Transformer(inputChannel = "mqttInboundChannel", outputChannel = "robotEventDedupFilterChannel")
    public RobotInboundMessage robotInboundTransformer(Message<String> message) {
        String topic = String.valueOf(message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
        String[] topicTokens = topic.split("/");
        String macAddress = topicTokens[2];
        String eventName = topicTokens[4];
        String payload = message.getPayload();

        log.info("MQTT inbound message 수신: topic = {}, payload = {}", topic, payload);

        try {
            RobotInboundPayload inboundPayload = StringUtils.hasText(payload)
                    ? objectMapper.readValue(payload, RobotInboundPayload.class)
                    : new RobotInboundPayload(null, null, null, null, null);
            return new RobotInboundMessage(macAddress, eventName, inboundPayload);
        } catch (Exception exception) {
            throw new IllegalArgumentException("MQTT inbound payload 역직렬화에 실패했습니다.", exception);
        }
    }

    @Filter(inputChannel = "robotEventDedupFilterChannel", outputChannel = "robotEventRouterChannel")
    public boolean robotEventDedupFilter(RobotInboundMessage inboundMessage) {
        boolean duplicated = robotEventDedupService.isDuplicatedRobotEvent(
                inboundMessage.payload().robotEventId(),
                inboundMessage.macAddress()
        );

        if (duplicated) {
            log.info("robot 이벤트 처리를 건너뜁니다: robotEventId = {}, eventName = {}, robotMacAddress = {}",
                    inboundMessage.payload().robotEventId(), inboundMessage.eventName(), inboundMessage.macAddress());
        }

        return !duplicated;
    }

    @Bean
    @Router(inputChannel = "robotEventRouterChannel")
    public AbstractMessageRouter robotEventRouter(
            MessageChannel robotConnectedChannel,
            MessageChannel robotDisconnectedChannel,
            MessageChannel robotArrivedChannel,
            MessageChannel robotReturnedChannel,
            MessageChannel robotEmergencyChannel
    ) {
        return new AbstractMessageRouter() {
            @Override
            protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
                RobotInboundMessage inboundMessage = (RobotInboundMessage) message.getPayload();

                return switch (inboundMessage.eventName()) {
                    case EVENT_CONNECTED -> List.of(robotConnectedChannel);
                    case EVENT_DISCONNECTED -> List.of(robotDisconnectedChannel);
                    case EVENT_ARRIVED -> List.of(robotArrivedChannel);
                    case EVENT_RETURNED -> List.of(robotReturnedChannel);
                    case EVENT_EMERGENCY -> List.of(robotEmergencyChannel);
                    default -> List.of();
                };
            }
        };
    }
}
