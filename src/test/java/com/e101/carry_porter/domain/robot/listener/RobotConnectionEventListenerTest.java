package com.e101.carry_porter.domain.robot.listener;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.e101.carry_porter.domain.robot.event.RobotConnectedMessageReceivedEvent;
import com.e101.carry_porter.domain.robot.event.RobotDisconnectedMessageReceivedEvent;
import com.e101.carry_porter.domain.robot.service.RobotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RobotConnectionEventListenerTest {

    @Mock
    private RobotService robotService;

    @InjectMocks
    private RobotConnectionEventListener robotConnectionEventListener;

    @Test
    @DisplayName("RobotConnectedMessageReceivedEvent를 수신하면 robotService.registerOrReconnect()를 한 번 호출한다")
    void handleRobotConnectedMessageReceivedEvent() {
        // given
        RobotConnectedMessageReceivedEvent event = new RobotConnectedMessageReceivedEvent("AA:BB:CC:DD:EE:10");

        // when
        robotConnectionEventListener.handleRobotConnectedMessageReceivedEvent(event);

        // then
        verify(robotService, times(1)).registerOrReconnect("AA:BB:CC:DD:EE:10");
    }

    @Test
    @DisplayName("RobotDisconnectedMessageReceivedEvent를 수신하면 robotService.disconnect()를 한 번 호출한다")
    void handleRobotDisconnectedMessageReceivedEvent() {
        // given
        RobotDisconnectedMessageReceivedEvent event = new RobotDisconnectedMessageReceivedEvent("AA:BB:CC:DD:EE:10");

        // when
        robotConnectionEventListener.handleRobotDisconnectedMessageReceivedEvent(event);

        // then
        verify(robotService, times(1)).disconnect("AA:BB:CC:DD:EE:10");
    }
}
