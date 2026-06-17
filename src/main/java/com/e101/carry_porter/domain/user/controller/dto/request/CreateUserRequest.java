package com.e101.carry_porter.domain.user.controller.dto.request;

import com.e101.carry_porter.domain.user.service.dto.request.CreateUserServiceRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "username은 비어 있을 수 없습니다.")
        @Size(max = 50, message = "username은 50자 이하여야 합니다.")
        String username,

        @NotBlank(message = "password는 비어 있을 수 없습니다.")
        @Size(min = 8, max = 255, message = "password는 8자 이상 255자 이하여야 합니다.")
        String password
) {

    public CreateUserServiceRequest toServiceRequest() {
        return new CreateUserServiceRequest(username, password);
    }
}
