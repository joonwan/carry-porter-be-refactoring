package com.e101.carry_porter.domain.notification.listener;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.e101.carry_porter.domain.notification.event.NotificationCreatedEvent;
import com.e101.carry_porter.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationDispatchEventListener notificationDispatchEventListener;

    @Test
    @DisplayName("NotificationCreatedEvent를 수신하면 notificationService.dispatch()를 한 번 호출한다")
    void handleNotificationCreatedEvent() {
        // given
        NotificationCreatedEvent event = new NotificationCreatedEvent(1L, 2L);

        // when
        notificationDispatchEventListener.handleNotificationCreatedEvent(event);

        // then
        verify(notificationService, times(1)).dispatch(1L);
    }
}
