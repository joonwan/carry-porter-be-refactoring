package com.e101.carry_porter.domain.robot.listener;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.e101.carry_porter.domain.mission.event.MissionReturnStartedEvent;
import com.e101.carry_porter.domain.mission.event.MissionStartedEvent;
import com.e101.carry_porter.domain.robot.service.MqttCommandPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RobotCommandEventListenerTest {

    @Mock
    private MqttCommandPublisher mqttCommandPublisher;

    @InjectMocks
    private RobotCommandEventListener robotCommandEventListener;

    @Test
    @DisplayName("MissionStartedEvent를 수신하면 출발 명령 발행 메서드를 한 번 호출한다")
    void handleMissionStartedEvent() {
        // given
        MissionStartedEvent event = new MissionStartedEvent(
                1L,
                2L,
                3L,
                "AA:BB:CC:DD:EE:01"
        );

        // when
        robotCommandEventListener.handleMissionStartedEvent(event);

        // then
        verify(mqttCommandPublisher, times(1))
                .publishDeparture(1L, 3L, "AA:BB:CC:DD:EE:01");
    }

    @Test
    @DisplayName("MissionReturnStartedEvent를 수신하면 복귀 명령 발행 메서드를 한 번 호출한다")
    void handleMissionReturnStartedEvent() {
        // given
        MissionReturnStartedEvent event = new MissionReturnStartedEvent(
                1L,
                2L,
                3L,
                "AA:BB:CC:DD:EE:01"
        );

        // when
        robotCommandEventListener.handleMissionReturnStartedEvent(event);

        // then
        verify(mqttCommandPublisher, times(1))
                .publishReturn(1L, 3L, "AA:BB:CC:DD:EE:01");
    }
}
