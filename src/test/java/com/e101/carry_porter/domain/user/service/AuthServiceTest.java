package com.e101.carry_porter.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.domain.user.exception.UserErrorCode;
import com.e101.carry_porter.domain.user.exception.UserException;
import com.e101.carry_porter.domain.user.repository.UserRepository;
import com.e101.carry_porter.domain.user.service.dto.request.LoginServiceRequest;
import com.e101.carry_porter.domain.user.service.dto.request.RefreshTokenServiceRequest;
import com.e101.carry_porter.domain.user.service.dto.response.LoginServiceResponse;
import com.e101.carry_porter.support.TransactionalIntegrationTestSupport;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest extends TransactionalIntegrationTestSupport {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("username 과 password가 일치하면 로그인에 성공한다")
    void login() {
        // given
        String encodedPassword = passwordEncoder.encode("password1234");
        User savedUser = userRepository.save(User.createUser("login-user", encodedPassword));
        LoginServiceRequest request = new LoginServiceRequest("login-user", "password1234");

        // when
        LoginServiceResponse response = authService.login(request);

        // then
        User loginUser = userRepository.findById(savedUser.getId()).orElseThrow();

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresAt()).isNotBlank();
        assertThat(loginUser.getRefreshToken()).isEqualTo(response.refreshToken());
        assertThatCode(() -> OffsetDateTime.parse(response.expiresAt())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("존재하지 않는 username 으로 로그인하면 UserException을 던진다")
    void loginWithInvalidUsername() {
        // given
        LoginServiceRequest request = new LoginServiceRequest("unknown-user", "password1234");

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 UserException을 던진다")
    void loginWithInvalidPassword() {
        // given
        String encodedPassword = passwordEncoder.encode("password1234");
        userRepository.save(User.createUser("login-user-2", encodedPassword));
        LoginServiceRequest request = new LoginServiceRequest("login-user-2", "wrong-password");

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("로그아웃하면 사용자의 refresh token 을 삭제한다")
    void logout() {
        // given
        String encodedPassword = passwordEncoder.encode("password1234");
        User user = userRepository.save(User.createUser("logout-user", encodedPassword));
        authService.login(new LoginServiceRequest("logout-user", "password1234"));

        // when
        authService.logout(user.getId());

        // then
        User logoutUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(logoutUser.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 로그아웃하면 UserException을 던진다")
    void logoutWithInvalidUser() {
        // when & then
        assertThatThrownBy(() -> authService.logout(9999L))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("유효한 refresh token 으로 요청하면 새 access token 과 새 refresh token 을 발급하고 DB 값을 갱신한다")
    void refresh() {
        // given
        String encodedPassword = passwordEncoder.encode("password1234");
        User savedUser = userRepository.save(User.createUser("refresh-user", encodedPassword));
        LoginServiceResponse loginResponse = authService.login(new LoginServiceRequest("refresh-user", "password1234"));
        RefreshTokenServiceRequest request = new RefreshTokenServiceRequest(loginResponse.refreshToken());

        // when
        LoginServiceResponse refreshResponse = authService.refresh(request);

        // then
        User refreshedUser = userRepository.findById(savedUser.getId()).orElseThrow();

        assertThat(refreshResponse.accessToken()).isNotBlank();
        assertThat(refreshResponse.refreshToken()).isNotBlank();
        assertThat(refreshResponse.refreshToken()).isNotEqualTo(loginResponse.refreshToken());
        assertThat(refreshedUser.getRefreshToken()).isEqualTo(refreshResponse.refreshToken());
        assertThat(refreshResponse.tokenType()).isEqualTo("Bearer");
        assertThatCode(() -> OffsetDateTime.parse(refreshResponse.expiresAt())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("유효하지 않은 refresh token 으로 요청하면 UserException을 던진다")
    void refreshWithInvalidToken() {
        // given
        RefreshTokenServiceRequest request = new RefreshTokenServiceRequest("invalid-refresh-token");

        // when & then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("저장된 refresh token 과 다르면 UserException을 던진다")
    void refreshWithMismatchedStoredToken() {
        // given
        String encodedPassword = passwordEncoder.encode("password1234");
        userRepository.save(User.createUser("refresh-user-2", encodedPassword));
        LoginServiceResponse firstLoginResponse = authService.login(new LoginServiceRequest("refresh-user-2", "password1234"));
        authService.login(new LoginServiceRequest("refresh-user-2", "password1234"));

        // when & then
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenServiceRequest(firstLoginResponse.refreshToken())))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_REFRESH_TOKEN);
    }
}
