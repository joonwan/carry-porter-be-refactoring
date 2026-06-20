package com.e101.carry_porter.domain.notification.service;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.mission.entity.MissionStatus;
import com.e101.carry_porter.domain.mission.repository.MissionRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    // SSE 연결을 30분 동안 유지
    private static final long DEFAULT_TIMEOUT = 30L * 60L * 1000L;
    private static final List<MissionStatus> ACTIVE_MISSION_STATUSES = List.of(
            MissionStatus.CREATED,
            MissionStatus.ASSIGNED,
            MissionStatus.DISPATCHED,
            MissionStatus.ARRIVED,
            MissionStatus.RETURNING
    );

    private final NotificationEmitterRepository notificationEmitterRepository;
    private final NotificationRepository notificationRepository;
    private final MissionRepository missionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SseEmitter createConnection(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // 동일 사용자의 기존 연결이 있으면 종료 후 새 연결로 교체
        notificationEmitterRepository.findByUserId(userId)
                .ifPresent(ResponseBodyEmitter::complete);

        notificationEmitterRepository.save(userId, emitter);
        registerEmitterCallbacks(userId, emitter);
        sendInitialMissionStateOrConnectEvent(userId, emitter);

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

    private void sendInitialMissionStateOrConnectEvent(Long userId, SseEmitter emitter) {
        missionRepository.findFirstByUserIdAndMissionStatusInOrderByIdDesc(userId, ACTIVE_MISSION_STATUSES)
                .ifPresentOrElse(
                        mission -> sendCurrentMissionStateEvent(userId, emitter, mission),
                        () -> sendConnectEvent(userId, emitter)
                );
    }

    private void sendCurrentMissionStateEvent(Long userId, SseEmitter emitter, Mission mission) {
        NotificationPayload payload = createCurrentMissionStatePayload(mission);

        try {
            log.info("현재 활성 미션 상태 동기화 전송: userId = {}, missionId = {}, eventType = {}",
                    userId, payload.missionId(), payload.eventType());
            emitter.send(SseEmitter.event()
                    .name(payload.eventType())
                    .data(payload));
        } catch (IOException exception) {
            log.error("현재 활성 미션 상태 동기화 전송 실패: userId = {}, missionId = {}",
                    userId, mission.getId(), exception);
            notificationEmitterRepository.delete(userId, emitter);
            emitter.completeWithError(exception);
        }
    }

    private NotificationPayload createCurrentMissionStatePayload(Mission mission) {
        return switch (mission.getMissionStatus()) {
            case CREATED -> NotificationPayload.of(
                    "MISSION_CREATED",
                    mission.getId(),
                    mission.getUser().getId(),
                    "미션 생성 요청이 접수되었습니다."
            );
            case ASSIGNED -> NotificationPayload.of(
                    "ROBOT_ASSIGNED",
                    mission.getId(),
                    mission.getUser().getId(),
                    "로봇 배정이 완료되었습니다."
            );
            case DISPATCHED -> NotificationPayload.of(
                    "MISSION_STARTED",
                    mission.getId(),
                    mission.getUser().getId(),
                    "로봇이 출발했습니다."
            );
            case ARRIVED -> NotificationPayload.of(
                    "MISSION_ARRIVED",
                    mission.getId(),
                    mission.getUser().getId(),
                    "로봇이 목적지에 도착했습니다."
            );
            case RETURNING -> NotificationPayload.of(
                    "MISSION_RETURN_STARTED",
                    mission.getId(),
                    mission.getUser().getId(),
                    "로봇이 복귀를 시작했습니다."
            );
            case FINISHED -> NotificationPayload.of(
                    "MISSION_FINISHED",
                    mission.getId(),
                    mission.getUser().getId(),
                    "미션이 완료되었습니다."
            );
            case FAILED -> NotificationPayload.failure(
                    mission.getId(),
                    mission.getUser().getId(),
                    "미션 수행 중 오류가 발생했습니다.",
                    null
            );
        };
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
