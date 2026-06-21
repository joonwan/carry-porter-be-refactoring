package com.e101.carry_porter.domain.notification.scheduler;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.e101.carry_porter.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SseHeartbeatSchedulerTest {

    @Mock
    private NotificationService notificationService;

    @Test
    @DisplayName("스케줄러가 실행되면 SSE heartbeat 전송을 서비스에 위임한다")
    void sendHeartbeat() {
        // given
        SseHeartbeatScheduler sseHeartbeatScheduler = new SseHeartbeatScheduler(notificationService);

        // when
        sseHeartbeatScheduler.sendHeartbeat();

        // then
        verify(notificationService, times(1)).sendHeartbeat();
    }
}
