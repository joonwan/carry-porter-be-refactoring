package com.e101.carry_porter.domain.user.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.e101.carry_porter.domain.user.controller.dto.request.CreateUserRequest;
import com.e101.carry_porter.domain.user.exception.UserErrorCode;
import com.e101.carry_porter.domain.user.exception.UserException;
import com.e101.carry_porter.domain.user.service.UserService;
import com.e101.carry_porter.domain.user.service.dto.response.CreateUserServiceResponse;
import com.e101.carry_porter.support.RestControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(UserController.class)
class UserControllerTest extends RestControllerTestSupport {

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("회원가입 요청이 유효하면 201 응답과 사용자 정보를 반환한다")
    void createUser() throws Exception {
        // given
        CreateUserRequest request = new CreateUserRequest("signup-user", "password1234");
        CreateUserServiceResponse response = new CreateUserServiceResponse(1L, "signup-user");

        given(userService.createUser(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("USER_CREATED"))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.username").value("signup-user"));
    }

    @Test
    @DisplayName("이미 존재하는 username 으로 회원가입 요청하면 409 응답을 반환한다")
    void createUserWithDuplicateUsername() throws Exception {
        // given
        CreateUserRequest request = new CreateUserRequest("duplicate-user", "password1234");

        given(userService.createUser(any()))
                .willThrow(new UserException(UserErrorCode.USERNAME_ALREADY_EXISTS));

        // when & then
        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_409"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 username 입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("username 이 비어 있으면 400 응답을 반환한다")
    void createUserWithBlankUsername() throws Exception {
        // given
        CreateUserRequest request = new CreateUserRequest("", "password1234");

        // when & then
        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400"))
                .andExpect(jsonPath("$.message").value("username은 비어 있을 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("password 길이가 8자 미만이면 400 응답을 반환한다")
    void createUserWithShortPassword() throws Exception {
        // given
        CreateUserRequest request = new CreateUserRequest("signup-user", "1234");

        // when & then
        mockMvc.perform(post("/api/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400"))
                .andExpect(jsonPath("$.message").value("password는 8자 이상 255자 이하여야 합니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
