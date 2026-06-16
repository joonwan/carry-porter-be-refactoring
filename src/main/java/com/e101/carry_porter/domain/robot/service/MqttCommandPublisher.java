package com.e101.carry_porter.domain.robot.service;

import com.e101.carry_porter.global.mqtt.MqttGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class MqttCommandPublisher {

    private final MqttGateway mqttGateway;

    public void publishDeparture(Long missionId, Long userId, String macAddress) {
        String topic = buildDepartureTopic(macAddress);
        String payload = buildDeparturePayload(missionId, userId);

        mqttGateway.publish(topic, payload);
        log.info("departure command 발행: mission id = {} robot mac addr = {}", missionId, macAddress);
    }

    public void publishReturn(Long missionId, Long userId, String macAddress) {
        String topic = buildReturnTopic(macAddress);
        String payload = buildReturnPayload(missionId, userId);

        mqttGateway.publish(topic, payload);
        log.info("return command 발행: mission id = {} robot mac addr = {}", missionId, macAddress);
    }

    private String buildDepartureTopic(String macAddress) {
        return "v1/robots/" + macAddress + "/command/departure";
    }

    private String buildReturnTopic(String macAddress) {
        return "v1/robots/" + macAddress + "/command/return";
    }

    private String buildDeparturePayload(Long missionId, Long userId) {
        return """
                {"missionId":%d,"userId":%d}
                """.formatted(missionId, userId);
    }

    private String buildReturnPayload(Long missionId, Long userId) {
        return """
                {"missionId":%d,"userId":%d}
                """.formatted(missionId, userId);
    }
}
