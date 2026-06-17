package com.e101.carry_porter.domain.notification.controller;

import com.e101.carry_porter.domain.notification.service.NotificationService;
import com.e101.carry_porter.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        log.info("SSE 구독 요청: userId = {}, username = {}",
                authenticatedUser.userId(), authenticatedUser.username());
        return notificationService.createConnection(authenticatedUser.userId());
    }
}
