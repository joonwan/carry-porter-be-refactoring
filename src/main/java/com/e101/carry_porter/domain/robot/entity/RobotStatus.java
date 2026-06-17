package com.e101.carry_porter.domain.robot.entity;

public enum RobotStatus {
    OFFLINE, // 서버와 연결되지 않아 배차할 수 없는 상태
    IDLE,    // 서버와 연결되어 있으며 배차 가능한 대기 상태
    BUSY     // 미션 수행 또는 복귀 중으로 배차가 불가능한 상태
}
