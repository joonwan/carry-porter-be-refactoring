package com.e101.carry_porter.domain.user.exception;

import com.e101.carry_porter.global.exception.GlobalException;

public class UserException extends GlobalException {

    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }
}
