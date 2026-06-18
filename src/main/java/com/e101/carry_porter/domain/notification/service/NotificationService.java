package com.e101.carry_porter.domain.notification.service;

import com.e101.carry_porter.domain.notification.dto.NotificationPayload;
import com.e101.carry_porter.domain.notification.entity.Notification;
import com.e101.carry_porter.domain.notification.event.NotificationCreatedEvent;
import com.e101.carry_porter.domain.notification.repository.NotificationEmitterRepository;
import com.e101.carry_porter.domain.notification.repository.NotificationRepository;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    // SSE 연결을 30분 동안 유지
    private static final long DEFAULT_TIMEOUT = 30L * 60L * 1000L;

    private final NotificationEmitterRepository notificationEmitterRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SseEmitter createConnection(Long userId, String lastEventIdHeader) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // 동일 사용자의 기존 연결이 있으면 종료 후 새 연결로 교체
        notificationEmitterRepository.findByUserId(userId)
                .ifPresent(ResponseBodyEmitter::complete);

        notificationEmitterRepository.save(userId, emitter);
        registerEmitterCallbacks(userId, emitter);
        sendConnectEvent(userId, emitter);
        replayMissedNotifications(userId, lastEventIdHeader, emitter);

        return emitter;
    }

    @Transactional
    public void createNotification(NotificationPayload payload) {
        Notification notification = notificationRepository.save(Notification.create(payload));

        log.info("알림 저장 완료: notificationId = {}, userId = {}, eventType = {}",
                notification.getId(), notification.getUserId(), notification.getEventType());

        eventPublisher.publishEvent(new NotificationCreatedEvent(
                notification.getId(),
                notification.getUserId()
        ));
    }

    public void dispatch(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        notificationEmitterRepository.findByUserId(notification.getUserId())
                .ifPresentOrElse(
                        emitter -> sendNotificationEvent(notification.getUserId(), emitter, notification),
                        () -> log.info("활성화된 SSE 연결이 없어 알림 전송을 건너뜁니다: userId = {}, eventType = {}",
                                notification.getUserId(), notification.getEventType())
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

    private void replayMissedNotifications(Long userId, String lastEventIdHeader, SseEmitter emitter) {
        Long lastEventId = parseLastEventId(lastEventIdHeader);

        if (lastEventId == null) {
            return;
        }

        List<Notification> missedNotifications =
                notificationRepository.findByUserIdAndIdGreaterThanOrderByIdAsc(userId, lastEventId);

        log.info("밀린 알림 재전송 시작: userId = {}, lastEventId = {}, missedCount = {}",
                userId, lastEventId, missedNotifications.size());

        for (Notification notification : missedNotifications) {
            sendNotificationEvent(userId, emitter, notification);
        }
    }

    private Long parseLastEventId(String lastEventIdHeader) {
        if (!StringUtils.hasText(lastEventIdHeader)) {
            return null;
        }

        try {
            return Long.parseLong(lastEventIdHeader);
        } catch (NumberFormatException exception) {
            log.warn("잘못된 Last-Event-ID 헤더를 무시합니다: value = {}", lastEventIdHeader);
            return null;
        }
    }

    private void sendNotificationEvent(Long userId, SseEmitter emitter, Notification notification) {
        NotificationPayload payload = NotificationPayload.from(notification);

        try {
            log.info("SSE 알림 전송: notificationId = {}, userId = {}, eventType = {}, missionId = {}",
                    notification.getId(), userId, payload.eventType(), payload.missionId());
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(notification.getId()))
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
