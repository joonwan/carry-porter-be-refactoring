package com.e101.carry_porter.domain.user.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.e101.carry_porter.domain.user.controller.dto.request.LoginRequest;
import com.e101.carry_porter.domain.user.controller.dto.request.RefreshTokenRequest;
import com.e101.carry_porter.domain.user.exception.UserErrorCode;
import com.e101.carry_porter.domain.user.exception.UserException;
import com.e101.carry_porter.domain.user.service.AuthService;
import com.e101.carry_porter.domain.user.service.dto.response.LoginServiceResponse;
import com.e101.carry_porter.global.security.AuthenticatedUser;
import com.e101.carry_porter.support.RestControllerTestSupport;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    @Test
    @DisplayName("인증된 사용자가 로그아웃 요청을 하면 200 응답을 반환한다")
    void logout() throws Exception {
        // given
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "logout-user");
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of()
        );

        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .with(authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOGOUT_SUCCESS"))
                .andExpect(jsonPath("$.message").value("로그아웃에 성공했습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));

        then(authService).should().logout(1L);
    }

    @Test
    @DisplayName("refresh token 요청이 유효하면 200 응답과 새 토큰을 반환한다")
    void refresh() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        LoginServiceResponse response = LoginServiceResponse.of(
                "new-access-token",
                "new-refresh-token",
                OffsetDateTime.parse("2026-06-19T00:00:00Z")
        );

        given(authService.refresh(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TOKEN_REFRESH_SUCCESS"))
                .andExpect(jsonPath("$.message").value("토큰 재발급에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresAt").value("2026-06-19T00:00Z"));
    }

    @Test
    @DisplayName("유효하지 않은 refresh token 으로 요청하면 401 응답을 반환한다")
    void refreshWithInvalidToken() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-refresh-token");

        given(authService.refresh(any()))
                .willThrow(new UserException(UserErrorCode.INVALID_REFRESH_TOKEN));

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_401"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 refresh token 입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("refresh token 이 비어 있으면 400 응답을 반환한다")
    void refreshWithBlankToken() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("");

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400"))
                .andExpect(jsonPath("$.message").value("refreshToken은 비어 있을 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
