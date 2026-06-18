package com.e101.carry_porter.domain.notification.repository;

import com.e101.carry_porter.domain.notification.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndIdGreaterThanOrderByIdAsc(Long userId, Long id);

    long deleteByCreatedAtBefore(LocalDateTime cutoffDateTime);
}
