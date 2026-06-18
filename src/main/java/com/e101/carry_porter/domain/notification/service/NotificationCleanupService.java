package com.e101.carry_porter.domain.notification.service;

import com.e101.carry_porter.domain.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationCleanupService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public long deleteNotificationsCreatedBefore(LocalDateTime cutoffDateTime) {
        long deletedCount = notificationRepository.deleteByCreatedAtBefore(cutoffDateTime);

        log.info("오래된 알림 삭제 완료: cutoffDateTime = {}, deletedCount = {}",
                cutoffDateTime, deletedCount);

        return deletedCount;
    }
}
