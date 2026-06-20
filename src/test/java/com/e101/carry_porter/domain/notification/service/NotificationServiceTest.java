package com.e101.carry_porter.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.mission.repository.MissionRepository;
import com.e101.carry_porter.domain.notification.dto.NotificationPayload;
import com.e101.carry_porter.domain.notification.entity.Notification;
import com.e101.carry_porter.domain.notification.event.NotificationCreatedEvent;
import com.e101.carry_porter.domain.notification.repository.NotificationEmitterRepository;
import com.e101.carry_porter.domain.notification.repository.NotificationRepository;
import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.domain.user.repository.UserRepository;
import com.e101.carry_porter.support.TransactionalIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationServiceTest extends TransactionalIntegrationTestSupport {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationEmitterRepository notificationEmitterRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEvents events;

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

    @Test
    @DisplayName("알림을 생성하면 DB에 저장하고 NotificationCreatedEvent를 발행한다")
    void createNotification() {
        // given
        Long userId = 1L;
        NotificationPayload payload = NotificationPayload.of(
                "MISSION_STARTED",
                10L,
                userId,
                "로봇이 출발했습니다."
        );

        // when
        notificationService.createNotification(payload);

        // then
        assertThat(notificationRepository.findAll()).hasSize(1);

        Notification notification = notificationRepository.findAll().getFirst();
        assertThat(notification.getUserId()).isEqualTo(userId);
        assertThat(notification.getMissionId()).isEqualTo(10L);
        assertThat(notification.getEventType()).isEqualTo("MISSION_STARTED");
        assertThat(notification.getMessage()).isEqualTo("로봇이 출발했습니다.");
        assertThat(events.stream(NotificationCreatedEvent.class)).hasSize(1);
        assertThat(events.stream(NotificationCreatedEvent.class).findFirst()).isPresent()
                .get()
                .extracting(NotificationCreatedEvent::notificationId, NotificationCreatedEvent::userId)
                .containsExactly(notification.getId(), userId);
    }

    @Test
    @DisplayName("활성화된 SSE 연결이 없어도 dispatch 호출 시 예외 없이 종료된다")
    void dispatchWithoutEmitter() {
        // given
        Notification notification = notificationRepository.save(
                Notification.create(NotificationPayload.of(
                        "MISSION_STARTED",
                        10L,
                        1L,
                        "로봇이 출발했습니다."
                ))
        );

        // when & then
        assertThatCode(() -> notificationService.dispatch(notification.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("종료되지 않은 미션이 있으면 SSE 구독 시 현재 활성 미션 상태를 동기화할 수 있도록 emitter를 연결한다")
    void createConnectionWithActiveMission() {
        // given
        User user = userRepository.save(User.createUser("active-user", "password1234"));
        missionRepository.save(Mission.createMission(user));

        // when
        SseEmitter emitter = notificationService.createConnection(user.getId());

        // then
        assertThat(emitter).isNotNull();
        assertThat(notificationEmitterRepository.findByUserId(user.getId())).contains(emitter);
    }
}
