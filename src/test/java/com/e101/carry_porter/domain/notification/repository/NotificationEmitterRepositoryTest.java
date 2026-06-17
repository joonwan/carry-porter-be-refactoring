package com.e101.carry_porter.domain.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationEmitterRepositoryTest {

    private final NotificationEmitterRepository notificationEmitterRepository = new NotificationEmitterRepository();

    @Test
    @DisplayName("같은 emitter 참조로 삭제 요청이 오면 저장소에서 제거한다")
    void deleteWithSameEmitterReference() {
        // given
        Long userId = 1L;
        SseEmitter emitter = new SseEmitter();
        notificationEmitterRepository.save(userId, emitter);

        // when
        notificationEmitterRepository.delete(userId, emitter);

        // then
        assertThat(notificationEmitterRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    @DisplayName("다른 emitter 참조로 삭제 요청이 와도 현재 저장된 emitter는 유지한다")
    void deleteWithDifferentEmitterReference() {
        // given
        Long userId = 1L;
        SseEmitter oldEmitter = new SseEmitter();
        SseEmitter newEmitter = new SseEmitter();
        notificationEmitterRepository.save(userId, newEmitter);

        // when
        notificationEmitterRepository.delete(userId, oldEmitter);

        // then
        assertThat(notificationEmitterRepository.findByUserId(userId)).contains(newEmitter);
    }
}
