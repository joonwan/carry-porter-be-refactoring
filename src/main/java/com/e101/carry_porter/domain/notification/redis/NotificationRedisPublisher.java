package com.e101.carry_porter.domain.notification.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic notificationTopic;

    public void publish(Long notificationId, Long userId) {
        NotificationRedisMessage message = new NotificationRedisMessage(notificationId, userId);

        redisTemplate.convertAndSend(notificationTopic.getTopic(), message);

        log.info("Redis Pub/Sub 알림 발행: channel = {}, notificationId = {}, userId = {}",
                notificationTopic.getTopic(), notificationId, userId);
    }
}
