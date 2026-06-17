package com.e101.carry_porter.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.e101.carry_porter.domain.notification.repository.NotificationEmitterRepository;
import com.e101.carry_porter.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationServiceTest extends IntegrationTestSupport {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationEmitterRepository notificationEmitterRepository;

    @Test
    @DisplayName("스프링 빈 환경에서도 SSE 구독 요청이 오면 emitter를 생성하여 저장소에 저장한다")
    void createConnection() {
        // given
        Long userId = 1L;

        // when
        SseEmitter emitter = notificationService.createConnection(userId);

        // then
        assertThat(emitter).isNotNull();
        assertThat(notificationEmitterRepository.findByUserId(userId)).contains(emitter);
    }

    @Test
    @DisplayName("같은 사용자가 다시 구독하면 기존 emitter를 종료하고 새 emitter로 교체한다")
    void createConnectionWithExistingEmitter() {
        // given
        Long userId = 1L;
        SseEmitter firstEmitter = notificationService.createConnection(userId);

        // when
        SseEmitter secondEmitter = notificationService.createConnection(userId);

        // then
        assertThat(secondEmitter).isNotNull();
        assertThat(secondEmitter).isNotSameAs(firstEmitter);
        assertThat(notificationEmitterRepository.findByUserId(userId)).contains(secondEmitter);
    }
}
