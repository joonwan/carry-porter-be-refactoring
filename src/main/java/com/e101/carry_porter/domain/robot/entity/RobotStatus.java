package com.e101.carry_porter.domain.robot.entity;

public enum RobotStatus {
    IDLE, // 배차 가능한 대기 상태
    BUSY  // 미션 수행 또는 복귀 중으로 배차가 불가능한 상태
}
