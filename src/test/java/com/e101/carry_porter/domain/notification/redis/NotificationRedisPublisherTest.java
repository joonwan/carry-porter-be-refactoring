package com.e101.carry_porter.domain.notification.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

@ExtendWith(MockitoExtension.class)
class NotificationRedisPublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ChannelTopic notificationTopic;

    @InjectMocks
    private NotificationRedisPublisher notificationRedisPublisher;

    @Captor
    private ArgumentCaptor<String> channelCaptor;

    @Captor
    private ArgumentCaptor<Object> messageCaptor;

    @Test
    @DisplayName("알림 발행 요청이 오면 Redis 채널에 notificationId와 userId를 담아 publish 한다")
    void publish() {
        // given
        Long notificationId = 1L;
        Long userId = 2L;
        given(notificationTopic.getTopic()).willReturn("carry-porter:notification");

        // when
        notificationRedisPublisher.publish(notificationId, userId);

        // then
        verify(redisTemplate, times(1)).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("carry-porter:notification");
        assertThat(messageCaptor.getValue()).isEqualTo(new NotificationRedisMessage(notificationId, userId));
    }
}
