package com.e101.carry_porter.domain.robot.service;

import com.e101.carry_porter.domain.robot.repository.ProcessedRobotEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RobotEventDedupService {

    private final ProcessedRobotEventRepository processedRobotEventRepository;

    public boolean isDuplicatedRobotEvent(String robotEventId, String robotMacAddress) {
        if (!StringUtils.hasText(robotEventId)) {
            log.warn("robotEventId 가 없어 robot 이벤트 처리를 차단합니다: robotMacAddress = {}", robotMacAddress);
            return true;
        }

        boolean duplicated = processedRobotEventRepository.existsByRobotEventId(robotEventId);

        if (duplicated) {
            log.info("이미 처리된 robot 이벤트입니다: robotEventId = {}, robotMacAddress = {}",
                    robotEventId, robotMacAddress);
        }

        return duplicated;
    }
}
