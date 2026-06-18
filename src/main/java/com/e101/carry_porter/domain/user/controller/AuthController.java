package com.e101.carry_porter.domain.user.controller;

import com.e101.carry_porter.domain.user.controller.dto.request.LoginRequest;
import com.e101.carry_porter.domain.user.service.AuthService;
import com.e101.carry_porter.domain.user.service.dto.response.LoginServiceResponse;
import com.e101.carry_porter.global.security.AuthenticatedUser;
import com.e101.carry_porter.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginServiceResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginServiceResponse response = authService.login(request.toServiceRequest());

        return ResponseEntity.ok(
                ApiResponse.success("LOGIN_SUCCESS", "로그인에 성공했습니다.", response)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        authService.logout(authenticatedUser.userId());

        return ResponseEntity.ok(
                ApiResponse.success("LOGOUT_SUCCESS", "로그아웃에 성공했습니다.", null)
        );
    }
}
