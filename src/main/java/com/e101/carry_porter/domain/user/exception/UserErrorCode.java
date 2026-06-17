package com.e101.carry_porter.domain.user.exception;

import com.e101.carry_porter.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "사용자를 찾을 수 없습니다."),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409", "이미 존재하는 username 입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "USER_401", "아이디 또는 비밀번호가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
