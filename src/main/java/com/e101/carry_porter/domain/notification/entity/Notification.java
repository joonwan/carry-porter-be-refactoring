package com.e101.carry_porter.domain.notification.entity;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @Column(nullable = false, length = 255)
    private String message;

    @Column(length = 100)
    private String failureCode;

    public static Notification create(
            User user,
            Mission mission,
            String eventType,
            String message,
            String failureCode
    ) {
        return Notification.builder()
                .user(user)
                .mission(mission)
                .eventType(eventType)
                .message(message)
                .failureCode(failureCode)
                .build();
    }

    @Builder
    private Notification(User user, Mission mission, String eventType, String message, String failureCode) {
        this.user = user;
        this.eventType = eventType;
        this.mission = mission;
        this.message = message;
        this.failureCode = failureCode;
    }

    public Long getUserId() {
        return user.getId();
    }

    public Long getMissionId() {
        if (mission == null) {
            return null;
        }

        return mission.getId();
    }
}
