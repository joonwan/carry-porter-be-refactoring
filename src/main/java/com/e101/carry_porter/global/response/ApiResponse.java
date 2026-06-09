package com.e101.carry_porter.global.response;

import com.e101.carry_porter.global.exception.ErrorCode;

public record ApiResponse<T>(
        String code,
        String message,
        T data
) {

    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
