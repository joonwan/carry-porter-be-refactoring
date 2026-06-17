package com.e101.carry_porter.domain.mission.exception;

import com.e101.carry_porter.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MissionErrorCode implements ErrorCode {

    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION_404", "미션을 찾을 수 없습니다."),
    MISSION_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "MISSION_409", "진행 중인 미션이 이미 존재합니다."),
    INVALID_MISSION_STATUS(HttpStatus.BAD_REQUEST, "MISSION_400", "현재 미션 상태에서는 로봇을 배정할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
