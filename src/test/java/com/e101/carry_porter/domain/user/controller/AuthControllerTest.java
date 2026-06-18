package com.e101.carry_porter.domain.user.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.e101.carry_porter.domain.user.controller.dto.request.LoginRequest;
import com.e101.carry_porter.domain.user.exception.UserErrorCode;
import com.e101.carry_porter.domain.user.exception.UserException;
import com.e101.carry_porter.domain.user.service.AuthService;
import com.e101.carry_porter.domain.user.service.dto.response.LoginServiceResponse;
import com.e101.carry_porter.support.RestControllerTestSupport;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends RestControllerTestSupport {

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("로그인 요청이 유효하면 200 응답과 access token을 반환한다")
    void login() throws Exception {
        // given
        LoginRequest request = new LoginRequest("login-user", "password1234");
        LoginServiceResponse response = LoginServiceResponse.of(
                "test-access-token",
                "test-refresh-token",
                OffsetDateTime.parse("2026-06-18T00:00:00Z")
        );

        given(authService.login(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresAt").value("2026-06-18T00:00Z"));
    }

    @Test
    @DisplayName("로그인 정보가 올바르지 않으면 401 응답을 반환한다")
    void loginWithInvalidCredential() throws Exception {
        // given
        LoginRequest request = new LoginRequest("login-user", "wrong-password");

        given(authService.login(any()))
                .willThrow(new UserException(UserErrorCode.LOGIN_FAILED));

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_401"))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("username 이 비어 있으면 400 응답을 반환한다")
    void loginWithBlankUsername() throws Exception {
        // given
        LoginRequest request = new LoginRequest("", "password1234");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400"))
                .andExpect(jsonPath("$.message").value("username은 비어 있을 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("password 길이가 8자 미만이면 400 응답을 반환한다")
    void loginWithShortPassword() throws Exception {
        // given
        LoginRequest request = new LoginRequest("login-user", "1234");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400"))
                .andExpect(jsonPath("$.message").value("password는 8자 이상 255자 이하여야 합니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
