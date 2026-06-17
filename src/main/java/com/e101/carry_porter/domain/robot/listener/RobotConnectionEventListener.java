package com.e101.carry_porter.domain.robot.listener;

import com.e101.carry_porter.domain.robot.event.RobotConnectedMessageReceivedEvent;
import com.e101.carry_porter.domain.robot.event.RobotDisconnectedMessageReceivedEvent;
import com.e101.carry_porter.domain.robot.service.RobotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotConnectionEventListener {

    private final RobotService robotService;

    @Async("eventTaskExecutor")
    @EventListener
    public void handleRobotConnectedMessageReceivedEvent(RobotConnectedMessageReceivedEvent event) {
        log.info("RobotConnectedMessageReceivedEvent 수신: robotMacAddress = {}", event.macAddress());
        robotService.registerOrReconnect(event.macAddress());
    }

    @Async("eventTaskExecutor")
    @EventListener
    public void handleRobotDisconnectedMessageReceivedEvent(RobotDisconnectedMessageReceivedEvent event) {
        log.info("RobotDisconnectedMessageReceivedEvent 수신: robotMacAddress = {}", event.macAddress());
        robotService.disconnect(event.macAddress());
    }
}
