package com.e101.carry_porter.domain.user.controller;

import com.e101.carry_porter.domain.user.controller.dto.request.CreateUserRequest;
import com.e101.carry_porter.domain.user.service.UserService;
import com.e101.carry_porter.domain.user.service.dto.response.CreateUserServiceResponse;
import com.e101.carry_porter.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateUserServiceResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        CreateUserServiceResponse response = userService.createUser(request.toServiceRequest());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("USER_CREATED", "회원가입이 완료되었습니다.", response));
    }
}
