package com.e101.carry_porter.domain.mission.exception;

import com.e101.carry_porter.global.exception.GlobalException;

public class MissionException extends GlobalException {

    public MissionException(MissionErrorCode errorCode) {
        super(errorCode);
    }
}
