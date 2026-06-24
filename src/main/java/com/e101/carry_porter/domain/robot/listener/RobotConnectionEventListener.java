package com.e101.carry_porter.domain.robot.listener;

import com.e101.carry_porter.domain.robot.event.RobotConnectedMessageReceivedEvent;
import com.e101.carry_porter.domain.robot.event.RobotDisconnectedMessageReceivedEvent;
import com.e101.carry_porter.domain.robot.exception.RobotErrorCode;
import com.e101.carry_porter.domain.robot.exception.RobotException;
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
        log.info("RobotConnectedMessageReceivedEvent 수신: robotEventId = {}, robotMacAddress = {}",
                event.robotEventId(), event.macAddress());
        try {
            robotService.registerOrReconnect(event.robotEventId(), event.macAddress());
        } catch (RobotException exception) {
            handleDuplicateRobotEvent(exception, event.robotEventId(), event.macAddress());
        }
    }

    @Async("eventTaskExecutor")
    @EventListener
    public void handleRobotDisconnectedMessageReceivedEvent(RobotDisconnectedMessageReceivedEvent event) {
        log.info("RobotDisconnectedMessageReceivedEvent 수신: robotEventId = {}, robotMacAddress = {}",
                event.robotEventId(), event.macAddress());
        try {
            robotService.disconnect(event.robotEventId(), event.macAddress());
        } catch (RobotException exception) {
            handleDuplicateRobotEvent(exception, event.robotEventId(), event.macAddress());
        }
    }

    private void handleDuplicateRobotEvent(RobotException exception, String robotEventId, String robotMacAddress) {
        if (exception.getErrorCode() != RobotErrorCode.DUPLICATE_ROBOT_EVENT) {
            throw exception;
        }

        log.info("이미 처리된 robot 이벤트이므로 robot 연결 상태 처리를 건너뜁니다: robotEventId = {}, robotMacAddress = {}",
                robotEventId, robotMacAddress);
    }
}
