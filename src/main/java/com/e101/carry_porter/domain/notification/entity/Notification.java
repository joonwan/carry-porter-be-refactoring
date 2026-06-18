package com.e101.carry_porter.domain.notification.entity;

import com.e101.carry_porter.domain.notification.dto.NotificationPayload;
import com.e101.carry_porter.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_id_id", columnList = "user_id, notification_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String eventType;

    private Long missionId;

    @Column(nullable = false, length = 255)
    private String message;

    @Column(length = 100)
    private String failureCode;

    public static Notification create(NotificationPayload payload) {
        return Notification.builder()
                .userId(payload.userId())
                .eventType(payload.eventType())
                .missionId(payload.missionId())
                .message(payload.message())
                .failureCode(payload.failureCode())
                .build();
    }

    @Builder
    private Notification(Long userId, String eventType, Long missionId, String message, String failureCode) {
        this.userId = userId;
        this.eventType = eventType;
        this.missionId = missionId;
        this.message = message;
        this.failureCode = failureCode;
    }
}
