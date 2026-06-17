package com.e101.carry_porter.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.domain.user.exception.UserErrorCode;
import com.e101.carry_porter.domain.user.exception.UserException;
import com.e101.carry_porter.domain.user.repository.UserRepository;
import com.e101.carry_porter.domain.user.service.dto.request.CreateUserServiceRequest;
import com.e101.carry_porter.domain.user.service.dto.response.CreateUserServiceResponse;
import com.e101.carry_porter.support.TransactionalIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest extends TransactionalIntegrationTestSupport {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("username 이 중복되지 않으면 비밀번호를 암호화하여 사용자를 저장한다")
    void createUser() {
        // given
        CreateUserServiceRequest request = new CreateUserServiceRequest("signup-user", "password1234");

        // when
        CreateUserServiceResponse response = userService.createUser(request);

        // then
        User savedUser = userRepository.findById(response.userId()).orElseThrow();

        assertThat(response.userId()).isNotNull();
        assertThat(response.username()).isEqualTo("signup-user");
        assertThat(savedUser.getUsername()).isEqualTo("signup-user");
        assertThat(savedUser.getPassword()).isNotEqualTo("password1234");
        assertThat(passwordEncoder.matches("password1234", savedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("이미 존재하는 username 이면 UserException을 던진다")
    void createUserWithDuplicateUsername() {
        // given
        userRepository.save(User.createUser("duplicate-user", "encoded-password"));
        CreateUserServiceRequest request = new CreateUserServiceRequest("duplicate-user", "password1234");

        // when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.USERNAME_ALREADY_EXISTS);
    }
}
