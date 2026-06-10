package com.e101.carry_porter.domain.robot.listener;

import com.e101.carry_porter.domain.mission.event.MissionCreatedEvent;
import com.e101.carry_porter.domain.robot.service.RobotService;
import com.e101.carry_porter.domain.robot.service.dto.request.AssignRobotServiceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotAssignmentEventListener {

    private final RobotService robotService;

    @EventListener
    public void handleMissionCreatedEvent(MissionCreatedEvent event) {
        log.info("MissionCreatedEvent 수신: missionId = {}. userId = {}",
                event.missionId(), event.userId());

        robotService.assignRobot(new AssignRobotServiceRequest(event.missionId()));
    }
}
