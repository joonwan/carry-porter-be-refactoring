package com.e101.carry_porter.domain.robot.exception;

import com.e101.carry_porter.global.exception.GlobalException;

public class RobotException extends GlobalException {

    public RobotException(RobotErrorCode errorCode) {
        super(errorCode);
    }
}
