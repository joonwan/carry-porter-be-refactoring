package com.e101.carry_porter.domain.mission.entity;

import com.e101.carry_porter.domain.robot.entity.Robot;
import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "missions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "robot_id")
    private Robot robot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionStatus missionStatus;

    public static Mission createMission(User user) {
        return Mission.builder()
                .user(user)
                .missionStatus(MissionStatus.CREATED)
                .build();
    }

    public void assignRobot(Robot robot) {
        this.robot = robot;
        this.missionStatus = MissionStatus.ASSIGNED;
        if (this.robot != null) {
            this.robot.toBusy();
        }
    }

    public void dispatch() {
        this.missionStatus = MissionStatus.DISPATCHED;
    }

    public void arrive() {
        this.missionStatus = MissionStatus.ARRIVED;
    }

    public void startReturning() {
        this.missionStatus = MissionStatus.RETURNING;
    }

    public void finish() {
        this.missionStatus = MissionStatus.FINISHED;
        if (this.robot != null) {
            this.robot.toIdle();
        }
    }

    public void fail() {
        this.missionStatus = MissionStatus.FAILED;
        if (this.robot != null) {
            this.robot.toIdle();
        }
    }

    @Builder
    private Mission(User user, Robot robot, MissionStatus missionStatus) {
        this.user = user;
        this.robot = robot;
        this.missionStatus = missionStatus;
    }
}
