package com.e101.carry_porter.domain.robot.entity;

import com.e101.carry_porter.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "processed_robot_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedRobotEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "processed_robot_event_id")
    private Long id;

    @Column(name = "robot_event_id", nullable = false, unique = true, length = 100)
    private String robotEventId;

    @Column(nullable = false, length = 50)
    private String robotMacAddress;

    public static ProcessedRobotEvent create(
            String robotEventId,
            String robotMacAddress
    ) {
        return ProcessedRobotEvent.builder()
                .robotEventId(robotEventId)
                .robotMacAddress(robotMacAddress)
                .build();
    }

    @Builder
    private ProcessedRobotEvent(
            String robotEventId,
            String robotMacAddress
    ) {
        this.robotEventId = robotEventId;
        this.robotMacAddress = robotMacAddress;
    }
}
