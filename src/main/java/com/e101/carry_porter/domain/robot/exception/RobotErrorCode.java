package com.e101.carry_porter.domain.robot.exception;

import com.e101.carry_porter.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RobotErrorCode implements ErrorCode {

    AVAILABLE_ROBOT_NOT_FOUND(HttpStatus.NOT_FOUND, "ROBOT_404", "배정 가능한 로봇이 없습니다."),
    ROBOT_NOT_FOUND(HttpStatus.NOT_FOUND, "ROBOT_405", "로봇을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
