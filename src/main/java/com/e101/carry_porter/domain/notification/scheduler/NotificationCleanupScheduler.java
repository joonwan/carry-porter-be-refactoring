package com.e101.carry_porter.domain.notification.scheduler;

import com.e101.carry_porter.domain.notification.config.NotificationCleanupProperties;
import com.e101.carry_porter.domain.notification.service.NotificationCleanupService;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final NotificationCleanupService notificationCleanupService;
    private final NotificationCleanupProperties notificationCleanupProperties;
    private final Clock clock;

    @Scheduled(cron = "${carry-porter.notification.cleanup.cron:0 0 3 * * *}")
    public void deleteExpiredNotifications() {
        LocalDateTime cutoffDateTime =
                LocalDateTime.now(clock).minusDays(notificationCleanupProperties.getRetentionDays());

        log.info("오래된 알림 삭제 스케줄 시작: retentionDays = {}, cutoffDateTime = {}",
                notificationCleanupProperties.getRetentionDays(), cutoffDateTime);

        long deletedCount = notificationCleanupService.deleteNotificationsCreatedBefore(cutoffDateTime);

        log.info("오래된 알림 삭제 스케줄 종료: deletedCount = {}", deletedCount);
    }
}
