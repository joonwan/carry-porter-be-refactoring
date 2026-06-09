package com.e101.carry_porter.domain.robot.entity;

import com.e101.carry_porter.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "robots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Robot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "robot_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String macAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RobotStatus robotStatus;

    public static Robot createRobot(String macAddress) {
        return Robot.builder()
                .macAddress(macAddress)
                .robotStatus(RobotStatus.IDLE)
                .build();
    }

    public void toIdle() {
        this.robotStatus = RobotStatus.IDLE;
    }

    public void toBusy() {
        this.robotStatus = RobotStatus.BUSY;
    }

    @Builder
    private Robot(String macAddress, RobotStatus robotStatus) {
        this.macAddress = macAddress;
        this.robotStatus = robotStatus;
    }
}
