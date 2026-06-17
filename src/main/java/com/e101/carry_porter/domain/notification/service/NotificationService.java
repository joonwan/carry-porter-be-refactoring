package com.e101.carry_porter.domain.notification.service;

import com.e101.carry_porter.domain.notification.dto.NotificationPayload;
import com.e101.carry_porter.domain.notification.repository.NotificationEmitterRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    // SSE 연결을 30분 동안 유지
    private static final long DEFAULT_TIMEOUT = 30L * 60L * 1000L;

    private final NotificationEmitterRepository notificationEmitterRepository;

    public SseEmitter createConnection(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // 동일 사용자의 기존 연결이 있으면 종료 후 새 연결로 교체
        notificationEmitterRepository.findByUserId(userId)
                .ifPresent(ResponseBodyEmitter::complete);

        notificationEmitterRepository.save(userId, emitter);
        registerEmitterCallbacks(userId, emitter);
        sendConnectEvent(userId, emitter);

        return emitter;
    }

    public void send(Long userId, NotificationPayload payload) {
        notificationEmitterRepository.findByUserId(userId)
                .ifPresentOrElse(
                        emitter -> sendNotificationEvent(userId, emitter, payload),
                        () -> log.info("활성화된 SSE 연결이 없어 알림 전송을 건너뜁니다: userId = {}, eventType = {}",
                                userId, payload.eventType())
                );
    }

    private void registerEmitterCallbacks(Long userId, SseEmitter emitter) {
        // 연결 종료, timeout, 에러 발생 시 저장소에서 emitter 제거
        emitter.onCompletion(() -> notificationEmitterRepository.delete(userId, emitter));
        emitter.onTimeout(() -> notificationEmitterRepository.delete(userId, emitter));
        emitter.onError(exception -> notificationEmitterRepository.delete(userId, emitter));
    }

    private void sendConnectEvent(Long userId, SseEmitter emitter) {
        try {
            // 구독 직후 연결이 열렸는지 확인할 수 있도록 초기 이벤트 전송
            log.info("연결 완료 이벤트 전송: userId = {}", userId);
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE connection established for userId=" + userId));
        } catch (IOException exception) {
            log.error("io exception 발생: userId = {}", userId);
            notificationEmitterRepository.delete(userId, emitter);
            emitter.completeWithError(exception);
        }
    }

    private void sendNotificationEvent(Long userId, SseEmitter emitter, NotificationPayload payload) {
        try {
            log.info("SSE 알림 전송: userId = {}, eventType = {}, missionId = {}",
                    userId, payload.eventType(), payload.missionId());
            emitter.send(SseEmitter.event()
                    .name(payload.eventType())
                    .data(payload));
        } catch (IOException exception) {
            log.error("SSE 알림 전송 실패: userId = {}, eventType = {}",
                    userId, payload.eventType(), exception);
            notificationEmitterRepository.delete(userId, emitter);
            emitter.completeWithError(exception);
        }
    }
}
