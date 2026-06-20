package com.e101.carry_porter.domain.notification.redis;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.e101.carry_porter.domain.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRedisSubscriberTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationRedisSubscriber notificationRedisSubscriber;

    @Test
    @DisplayName("Redis Pub/Sub 메시지를 수신하면 LinkedHashMap 이어도 NotificationRedisMessage로 변환해 dispatch()를 호출한다")
    void handleMessage() {
        // given
        Map<String, Object> message = Map.of(
                "notificationId", 1L,
                "userId", 2L
        );

        // when
        notificationRedisSubscriber.handleMessage(message);

        // then
        verify(notificationService, times(1)).dispatch(1L);
    }
}
