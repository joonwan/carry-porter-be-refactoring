package com.e101.carry_porter.domain.notification.listener;

import com.e101.carry_porter.domain.notification.event.NotificationCreatedEvent;
import com.e101.carry_porter.domain.notification.redis.NotificationRedisPublisher;
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

    private final NotificationRedisPublisher notificationRedisPublisher;

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreatedEvent(NotificationCreatedEvent event) {
        log.info("NotificationCreatedEvent 수신: notificationId = {}, userId = {}",
                event.notificationId(), event.userId());
        notificationRedisPublisher.publish(event.notificationId(), event.userId());
    }
}
