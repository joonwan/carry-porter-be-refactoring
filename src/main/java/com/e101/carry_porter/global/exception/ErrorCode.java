package com.e101.carry_porter.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String getCode();

    String getMessage();

    HttpStatus getStatus();
}
