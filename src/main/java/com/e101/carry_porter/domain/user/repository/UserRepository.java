package com.e101.carry_porter.domain.user.repository;

import com.e101.carry_porter.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
