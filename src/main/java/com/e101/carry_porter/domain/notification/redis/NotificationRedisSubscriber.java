package com.e101.carry_porter.domain.notification.redis;

import com.e101.carry_porter.domain.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public void handleMessage(Object message) {
        NotificationRedisMessage notificationRedisMessage = objectMapper.convertValue(
                message,
                NotificationRedisMessage.class
        );

        log.info("Redis Pub/Sub 알림 수신: notificationId = {}, userId = {}",
                notificationRedisMessage.notificationId(), notificationRedisMessage.userId());

        notificationService.dispatch(notificationRedisMessage.notificationId());
    }
}
