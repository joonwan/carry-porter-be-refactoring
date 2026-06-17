package com.e101.carry_porter.domain.mission.repository;

import com.e101.carry_porter.domain.mission.entity.Mission;
import com.e101.carry_porter.domain.mission.entity.MissionStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    boolean existsByUserIdAndMissionStatusIn(Long userId, Collection<MissionStatus> missionStatuses);
}
