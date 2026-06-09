package com.e101.carry_porter.domain.mission.entity;

public enum MissionStatus {
    CREATED,    // 사용자 호출 요청이 생성된 상태
    ASSIGNED,   // 수행할 로봇이 배정된 상태
    DISPATCHED, // 로봇이 출발 명령을 받고 이동을 시작한 상태
    ARRIVED,    // 로봇이 사용자 위치 또는 목적지에 도착한 상태
    RETURNING,  // 로봇이 복귀 지점으로 이동 중인 상태
    FINISHED,   // 복귀까지 완료되어 미션이 정상 종료된 상태
    FAILED      // 미션 수행 중 복구 불가능한 실패가 발생한 상태
}
