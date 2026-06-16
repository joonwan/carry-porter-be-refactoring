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

    private String buildDepartureTopic(String macAddress) {
        return "v1/robots/" + macAddress + "/command/departure";
    }

    private String buildDeparturePayload(Long missionId, Long userId) {
        return """
                {"missionId":%d,"userId":%d}
                """.formatted(missionId, userId);
    }
}
