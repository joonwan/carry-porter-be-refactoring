package com.e101.carry_porter.domain.notification.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.e101.carry_porter.domain.notification.service.NotificationService;
import com.e101.carry_porter.global.security.AuthenticatedUser;
import com.e101.carry_porter.support.RestControllerTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest extends RestControllerTestSupport {

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName("인증된 사용자가 SSE 구독 요청을 하면 emitter를 반환한다")
    void subscribe() throws Exception {
        // given
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "notification-user");
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of()
        );
        SseEmitter emitter = new SseEmitter();

        given(notificationService.createConnection(eq(1L))).willReturn(emitter);

        // when & then
        mockMvc.perform(get("/api/notifications/subscribe")
                        .with(authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        then(notificationService).should().createConnection(1L);
    }
}
