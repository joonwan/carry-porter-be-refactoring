package com.e101.carry_porter.domain.robot.repository;

import com.e101.carry_porter.domain.robot.entity.Robot;
import com.e101.carry_porter.domain.robot.entity.RobotStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RobotRepository extends JpaRepository<Robot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Robot> findFirstByRobotStatusOrderByIdAsc(RobotStatus robotStatus);
}
