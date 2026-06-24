package com.e101.carry_porter.domain.robot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.e101.carry_porter.domain.robot.entity.ProcessedRobotEvent;
import com.e101.carry_porter.domain.robot.exception.RobotErrorCode;
import com.e101.carry_porter.domain.robot.exception.RobotException;
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

    @Test
    @DisplayName("처리가 완료된 robotEventId를 저장하면 processed_robot_events 테이블에 저장된다")
    void markProcessedRobotEvent() {
        // given
        String robotEventId = "robot-event-3";

        // when
        robotEventDedupService.markProcessedRobotEvent(robotEventId, "AA:BB:CC:DD:EE:03");

        // then
        assertThat(processedRobotEventRepository.existsByRobotEventId(robotEventId)).isTrue();
    }

    @Test
    @DisplayName("이미 처리된 robotEventId를 다시 저장하면 RobotException을 던진다")
    void markProcessedRobotEventWithDuplicateRobotEventId() {
        // given
        String robotEventId = "robot-event-duplicate-1";
        processedRobotEventRepository.saveAndFlush(
                ProcessedRobotEvent.create(robotEventId, "AA:BB:CC:DD:EE:05")
        );

        // when & then
        assertThatThrownBy(() -> robotEventDedupService.markProcessedRobotEvent(
                robotEventId,
                "AA:BB:CC:DD:EE:05"
        ))
                .isInstanceOf(RobotException.class)
                .extracting(exception -> ((RobotException) exception).getErrorCode())
                .isEqualTo(RobotErrorCode.DUPLICATE_ROBOT_EVENT);
    }

    @Test
    @DisplayName("robotEventId가 비어 있으면 처리 완료 저장 시 IllegalArgumentException을 던진다")
    void markProcessedRobotEventWithBlankRobotEventId() {
        // given
        String blankRobotEventId = " ";

        // when & then
        assertThatThrownBy(() -> robotEventDedupService.markProcessedRobotEvent(
                blankRobotEventId,
                "AA:BB:CC:DD:EE:04"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
