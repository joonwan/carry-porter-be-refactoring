package com.e101.carry_porter.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.mission.repository.MissionRepository;
import com.e101.carry_porter.domain.notification.entity.Notification;
import com.e101.carry_porter.domain.notification.repository.NotificationRepository;
import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.domain.user.repository.UserRepository;
import com.e101.carry_porter.support.TransactionalIntegrationTestSupport;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class NotificationCleanupServiceTest extends TransactionalIntegrationTestSupport {

    @Autowired
    private NotificationCleanupService notificationCleanupService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("기준 시각보다 오래된 알림만 삭제한다")
    void deleteNotificationsCreatedBefore() {
        // given
        User user = userRepository.save(User.createUser("notification-cleanup-user", "password1234"));
        Mission expiredMission = missionRepository.save(Mission.createMission(user));
        Mission recentMission = missionRepository.save(Mission.createMission(user));
        Notification expiredNotification = notificationRepository.saveAndFlush(
                Notification.create(
                        user,
                        expiredMission,
                        "MISSION_STARTED",
                        "오래된 알림입니다.",
                        null
                )
        );
        Notification recentNotification = notificationRepository.saveAndFlush(
                Notification.create(
                        user,
                        recentMission,
                        "MISSION_FINISHED",
                        "최근 알림입니다.",
                        null
                )
        );

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffDateTime = now.minusDays(3);
        updateCreatedAt(expiredNotification.getId(), now.minusDays(4));
        updateCreatedAt(recentNotification.getId(), now.minusDays(2));

        // when
        long deletedCount = notificationCleanupService.deleteNotificationsCreatedBefore(cutoffDateTime);

        // then
        assertThat(deletedCount).isEqualTo(1);
        assertThat(notificationRepository.findById(expiredNotification.getId())).isEmpty();
        assertThat(notificationRepository.findById(recentNotification.getId())).isPresent();
    }

    private void updateCreatedAt(Long notificationId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "update notifications set created_at = ? where notification_id = ?",
                Timestamp.valueOf(createdAt),
                notificationId
        );
    }
}
