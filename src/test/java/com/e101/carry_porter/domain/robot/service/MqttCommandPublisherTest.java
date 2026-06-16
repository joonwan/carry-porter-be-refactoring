package com.e101.carry_porter.domain.robot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.e101.carry_porter.global.mqtt.MqttGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MqttCommandPublisherTest {

    @Mock
    private MqttGateway mqttGateway;

    @InjectMocks
    private MqttCommandPublisher mqttCommandPublisher;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> payloadCaptor;

    @Test
    @DisplayName("출발 명령을 발행하면 mac address 기반 topic과 payload로 MQTT publish를 호출한다")
    void publishDeparture() {
        // given
        Long missionId = 1L;
        Long userId = 2L;
        String macAddress = "AA:BB:CC:DD:EE:01";

        // when
        mqttCommandPublisher.publishDeparture(missionId, userId, macAddress);

        // then
        verify(mqttGateway, times(1)).publish(topicCaptor.capture(), payloadCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("v1/robots/AA:BB:CC:DD:EE:01/command/departure");
        assertThat(payloadCaptor.getValue())
                .contains("\"missionId\":1")
                .contains("\"userId\":2")
                .doesNotContain("\"command\"");
    }
}
