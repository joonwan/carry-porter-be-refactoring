package com.e101.carry_porter.domain.robot.repository;

import com.e101.carry_porter.domain.robot.entity.ProcessedRobotEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedRobotEventRepository extends JpaRepository<ProcessedRobotEvent, Long> {

    boolean existsByRobotEventId(String robotEventId);

    Optional<ProcessedRobotEvent> findByRobotEventId(String robotEventId);
}
