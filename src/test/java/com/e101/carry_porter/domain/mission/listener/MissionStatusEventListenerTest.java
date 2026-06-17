package com.e101.carry_porter.domain.mission.listener;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.e101.carry_porter.domain.mission.service.MissionService;
import com.e101.carry_porter.domain.robot.event.RobotArrivedMessageReceivedEvent;
import com.e101.carry_porter.domain.robot.event.RobotAssignedEvent;
import com.e101.carry_porter.domain.robot.event.RobotAssignmentFailedEvent;
import com.e101.carry_porter.domain.robot.event.RobotEmergencyMessageReceivedEvent;
import com.e101.carry_porter.domain.robot.event.RobotReturnedMessageReceivedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MissionStatusEventListenerTest {

    @Mock
    private MissionService missionService;

    @InjectMocks
    private MissionStatusEventListener missionStatusEventListener;

    @Test
    @DisplayName("RobotAssignedEvent를 수신하면 missionService.dispatch()를 한 번 호출한다")
    void handleRobotAssignedEvent() {
        // given
        RobotAssignedEvent event = new RobotAssignedEvent(1L, 2L, 3L);

        // when
        missionStatusEventListener.handleRobotAssignedEvent(event);

        // then
        verify(missionService, times(1)).dispatch(1L, 2L, 3L);
    }

    @Test
    @DisplayName("RobotAssignmentFailedEvent를 수신하면 missionService.failAssignment()를 한 번 호출한다")
    void handleRobotAssignmentFailedEvent() {
        // given
        RobotAssignmentFailedEvent event = new RobotAssignmentFailedEvent(
                1L,
                3L,
                "ROBOT_404",
                "배정 가능한 로봇이 없습니다."
        );

        // when
        missionStatusEventListener.handleRobotAssignmentFailedEvent(event);

        // then
        verify(missionService, times(1)).failAssignment(
                1L,
                3L,
                "ROBOT_404",
                "배정 가능한 로봇이 없습니다."
        );
    }

    @Test
    @DisplayName("RobotArrivedMessageReceivedEvent를 수신하면 missionService.arrive()를 한 번 호출한다")
    void handleRobotArrivedMessageReceivedEvent() {
        // given
        RobotArrivedMessageReceivedEvent event = new RobotArrivedMessageReceivedEvent(1L, "AA:BB:CC:DD:EE:01", 3L);

        // when
        missionStatusEventListener.handleRobotArrivedMessageReceivedEvent(event);

        // then
        verify(missionService, times(1)).arrive(1L, "AA:BB:CC:DD:EE:01", 3L);
    }

    @Test
    @DisplayName("RobotReturnedMessageReceivedEvent를 수신하면 missionService.finish()를 한 번 호출한다")
    void handleRobotReturnedMessageReceivedEvent() {
        // given
        RobotReturnedMessageReceivedEvent event = new RobotReturnedMessageReceivedEvent(1L, "AA:BB:CC:DD:EE:01", 3L);

        // when
        missionStatusEventListener.handleRobotReturnedMessageReceivedEvent(event);

        // then
        verify(missionService, times(1)).finish(1L, "AA:BB:CC:DD:EE:01", 3L);
    }

    @Test
    @DisplayName("RobotEmergencyMessageReceivedEvent를 수신하면 missionService.fail()을 한 번 호출한다")
    void handleRobotEmergencyMessageReceivedEvent() {
        // given
        RobotEmergencyMessageReceivedEvent event = new RobotEmergencyMessageReceivedEvent(
                1L,
                "AA:BB:CC:DD:EE:01",
                3L,
                "ROBOT_EMERGENCY",
                "obstacle detected"
        );

        // when
        missionStatusEventListener.handleRobotEmergencyMessageReceivedEvent(event);

        // then
        verify(missionService, times(1)).fail(
                1L,
                "AA:BB:CC:DD:EE:01",
                3L,
                "ROBOT_EMERGENCY",
                "obstacle detected"
        );
    }
}
