package com.e101.carry_porter.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.domain.user.exception.UserErrorCode;
import com.e101.carry_porter.domain.user.exception.UserException;
import com.e101.carry_porter.domain.user.repository.UserRepository;
import com.e101.carry_porter.domain.user.service.dto.request.LoginServiceRequest;
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
        userRepository.save(User.createUser("login-user", encodedPassword));
        LoginServiceRequest request = new LoginServiceRequest("login-user", "password1234");

        // when
        LoginServiceResponse response = authService.login(request);

        // then
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresAt()).isNotBlank();
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
}
