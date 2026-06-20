package com.e101.carry_porter.domain.robot.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.e101.carry_porter.domain.robot.entity.ProcessedRobotEvent;
import com.e101.carry_porter.domain.robot.repository.ProcessedRobotEventRepository;
import com.e101.carry_porter.support.TransactionalIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RobotEventDedupServiceTest extends TransactionalIntegrationTestSupport {

    @Autowired
    private RobotEventDedupService robotEventDedupService;

    @Autowired
    private ProcessedRobotEventRepository processedRobotEventRepository;

    @Test
    @DisplayName("처리 이력이 없는 robotEventId이면 중복이 아니므로 false를 반환한다")
    void isDuplicatedRobotEventWithNotProcessedRobotEvent() {
        // given
        String robotEventId = "robot-event-1";

        // when
        boolean duplicated = robotEventDedupService.isDuplicatedRobotEvent(
                robotEventId,
                "AA:BB:CC:DD:EE:01"
        );

        // then
        assertThat(duplicated).isFalse();
    }

    @Test
    @DisplayName("이미 처리 이력이 있는 robotEventId이면 중복이므로 true를 반환한다")
    void isDuplicatedRobotEventWithProcessedRobotEvent() {
        // given
        String robotEventId = "robot-event-2";
        processedRobotEventRepository.saveAndFlush(
                ProcessedRobotEvent.create(robotEventId, "AA:BB:CC:DD:EE:02")
        );

        // when
        boolean duplicated = robotEventDedupService.isDuplicatedRobotEvent(
                robotEventId,
                "AA:BB:CC:DD:EE:02"
        );

        // then
        assertThat(duplicated).isTrue();
    }

    @Test
    @DisplayName("robotEventId가 비어 있으면 유효하지 않은 메시지로 간주하고 true를 반환한다")
    void isDuplicatedRobotEventWithBlankRobotEventId() {
        // given
        String blankRobotEventId = " ";

        // when
        boolean duplicated = robotEventDedupService.isDuplicatedRobotEvent(
                blankRobotEventId,
                "AA:BB:CC:DD:EE:03"
        );

        // then
        assertThat(duplicated).isTrue();
    }
}
