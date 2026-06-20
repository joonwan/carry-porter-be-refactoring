package com.e101.carry_porter.domain.notification.listener;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.e101.carry_porter.domain.notification.event.NotificationCreatedEvent;
import com.e101.carry_porter.domain.notification.redis.NotificationRedisPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchEventListenerTest {

    @Mock
    private NotificationRedisPublisher notificationRedisPublisher;

    @InjectMocks
    private NotificationDispatchEventListener notificationDispatchEventListener;

    @Test
    @DisplayName("NotificationCreatedEvent를 수신하면 notificationRedisPublisher.publish()를 한 번 호출한다")
    void handleNotificationCreatedEvent() {
        // given
        NotificationCreatedEvent event = new NotificationCreatedEvent(1L, 2L);

        // when
        notificationDispatchEventListener.handleNotificationCreatedEvent(event);

        // then
        verify(notificationRedisPublisher, times(1)).publish(1L, 2L);
    }
}
