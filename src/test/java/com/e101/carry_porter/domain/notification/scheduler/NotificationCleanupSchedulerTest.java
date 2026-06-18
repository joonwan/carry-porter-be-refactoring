package com.e101.carry_porter.domain.notification.scheduler;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.e101.carry_porter.domain.notification.config.NotificationCleanupProperties;
import com.e101.carry_porter.domain.notification.service.NotificationCleanupService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerTest {

    @Mock
    private NotificationCleanupService notificationCleanupService;

    @Test
    @DisplayName("스케줄러가 실행되면 retentionDays 기준 시각을 계산해 오래된 알림 삭제를 호출한다")
    void deleteExpiredNotifications() {
        // given
        NotificationCleanupProperties properties = new NotificationCleanupProperties();
        properties.setRetentionDays(3);
        properties.setCron("0 0 3 * * *");
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-06-18T00:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        NotificationCleanupScheduler notificationCleanupScheduler =
                new NotificationCleanupScheduler(notificationCleanupService, properties, fixedClock);

        // when
        notificationCleanupScheduler.deleteExpiredNotifications();

        // then
        verify(notificationCleanupService, times(1))
                .deleteNotificationsCreatedBefore(LocalDateTime.of(2026, 6, 15, 9, 0, 0));
    }
}
