package com.e101.carry_porter.domain.notification.scheduler;

import com.e101.carry_porter.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

    private final NotificationService notificationService;

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        notificationService.sendHeartbeat();
    }
}
