package com.e101.carry_porter.domain.robot.repository;

import com.e101.carry_porter.domain.robot.entity.Robot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RobotRepository extends JpaRepository<Robot, Long> {
}
