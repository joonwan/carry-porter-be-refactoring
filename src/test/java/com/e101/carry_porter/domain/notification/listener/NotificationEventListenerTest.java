package com.e101.carry_porter.domain.notification.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.e101.carry_porter.domain.mission.event.MissionArrivedEvent;
import com.e101.carry_porter.domain.mission.event.MissionFailedEvent;
import com.e101.carry_porter.domain.mission.event.MissionFinishedEvent;
import com.e101.carry_porter.domain.mission.event.MissionReturnStartedEvent;
import com.e101.carry_porter.domain.mission.event.MissionStartedEvent;
import com.e101.carry_porter.domain.notification.dto.NotificationPayload;
import com.e101.carry_porter.domain.notification.service.NotificationService;
import com.e101.carry_porter.domain.robot.event.RobotAssignedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    @Test
    @DisplayName("RobotAssignedEvent를 수신하면 로봇 배정 알림을 전송한다")
    void handleRobotAssignedEvent() {
        // given
        RobotAssignedEvent event = new RobotAssignedEvent(1L, 2L, 3L);

        // when
        notificationEventListener.handleRobotAssignedEvent(event);

        // then
        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService, times(1)).createNotification(payloadCaptor.capture());
        verifyNotificationPayload(payloadCaptor.getValue(), "ROBOT_ASSIGNED", 1L, 3L, "로봇 배정이 완료되었습니다.", null);
    }

    @Test
    @DisplayName("MissionStartedEvent를 수신하면 출발 알림을 전송한다")
    void handleMissionStartedEvent() {
        // given
        MissionStartedEvent event = new MissionStartedEvent(1L, 2L, 3L, "AA:BB:CC:DD:EE:01");

        // when
        notificationEventListener.handleMissionStartedEvent(event);

        // then
        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService, times(1)).createNotification(payloadCaptor.capture());
        verifyNotificationPayload(payloadCaptor.getValue(), "MISSION_STARTED", 1L, 3L, "로봇이 출발했습니다.", null);
    }

    @Test
    @DisplayName("MissionArrivedEvent를 수신하면 도착 알림을 전송한다")
    void handleMissionArrivedEvent() {
        // given
        MissionArrivedEvent event = new MissionArrivedEvent(1L, "AA:BB:CC:DD:EE:01", 3L);

        // when
        notificationEventListener.handleMissionArrivedEvent(event);

        // then
        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService, times(1)).createNotification(payloadCaptor.capture());
        verifyNotificationPayload(payloadCaptor.getValue(), "MISSION_ARRIVED", 1L, 3L, "로봇이 목적지에 도착했습니다.", null);
    }

    @Test
    @DisplayName("MissionReturnStartedEvent를 수신하면 복귀 시작 알림을 전송한다")
    void handleMissionReturnStartedEvent() {
        // given
        MissionReturnStartedEvent event = new MissionReturnStartedEvent(1L, 2L, 3L, "AA:BB:CC:DD:EE:01");

        // when
        notificationEventListener.handleMissionReturnStartedEvent(event);

        // then
        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService, times(1)).createNotification(payloadCaptor.capture());
        verifyNotificationPayload(payloadCaptor.getValue(), "MISSION_RETURN_STARTED", 1L, 3L, "로봇이 복귀를 시작했습니다.", null);
    }

    @Test
    @DisplayName("MissionFinishedEvent를 수신하면 완료 알림을 전송한다")
    void handleMissionFinishedEvent() {
        // given
        MissionFinishedEvent event = new MissionFinishedEvent(1L, "AA:BB:CC:DD:EE:01", 3L);

        // when
        notificationEventListener.handleMissionFinishedEvent(event);

        // then
        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService, times(1)).createNotification(payloadCaptor.capture());
        verifyNotificationPayload(payloadCaptor.getValue(), "MISSION_FINISHED", 1L, 3L, "미션이 완료되었습니다.", null);
    }

    @Test
    @DisplayName("MissionFailedEvent를 수신하면 실패 알림을 전송한다")
    void handleMissionFailedEvent() {
        // given
        MissionFailedEvent event = new MissionFailedEvent(
                1L,
                "AA:BB:CC:DD:EE:01",
                3L,
                "ROBOT_EMERGENCY",
                "obstacle detected"
        );

        // when
        notificationEventListener.handleMissionFailedEvent(event);

        // then
        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService, times(1)).createNotification(payloadCaptor.capture());
        verifyNotificationPayload(payloadCaptor.getValue(), "MISSION_FAILED", 1L, 3L, "obstacle detected", "ROBOT_EMERGENCY");
    }

    private void verifyNotificationPayload(
            NotificationPayload payload,
            String eventType,
            Long missionId,
            Long userId,
            String message,
            String failureCode
    ) {
        assertThat(payload.eventType()).isEqualTo(eventType);
        assertThat(payload.missionId()).isEqualTo(missionId);
        assertThat(payload.userId()).isEqualTo(userId);
        assertThat(payload.message()).isEqualTo(message);
        assertThat(payload.failureCode()).isEqualTo(failureCode);
    }
}
