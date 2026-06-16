package com.e101.carry_porter.domain.mission.listener;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.e101.carry_porter.domain.mission.event.MissionArrivedEvent;
import com.e101.carry_porter.domain.mission.event.MissionFinishedEvent;
import com.e101.carry_porter.domain.mission.service.MissionService;
import com.e101.carry_porter.domain.robot.event.RobotAssignedEvent;
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
    @DisplayName("MissionArrivedEvent를 수신하면 missionService.arrive()를 한 번 호출한다")
    void handleMissionArrivedEvent() {
        // given
        MissionArrivedEvent event = new MissionArrivedEvent(1L, "AA:BB:CC:DD:EE:01", 3L);

        // when
        missionStatusEventListener.handleMissionArrivedEvent(event);

        // then
        verify(missionService, times(1)).arrive(1L, "AA:BB:CC:DD:EE:01", 3L);
    }

    @Test
    @DisplayName("MissionFinishedEvent를 수신하면 missionService.finish()를 한 번 호출한다")
    void handleMissionFinishedEvent() {
        // given
        MissionFinishedEvent event = new MissionFinishedEvent(1L, "AA:BB:CC:DD:EE:01", 3L);

        // when
        missionStatusEventListener.handleMissionFinishedEvent(event);

        // then
        verify(missionService, times(1)).finish(1L, "AA:BB:CC:DD:EE:01", 3L);
    }
}
