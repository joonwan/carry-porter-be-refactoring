package com.e101.carry_porter.domain.notification.listener;

import com.e101.carry_porter.domain.notification.event.NotificationCreatedEvent;
import com.e101.carry_porter.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatchEventListener {

    private final NotificationService notificationService;

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreatedEvent(NotificationCreatedEvent event) {
        log.info("NotificationCreatedEvent 수신: notificationId = {}, userId = {}",
                event.notificationId(), event.userId());
        notificationService.dispatch(event.notificationId());
    }
}
