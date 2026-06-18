package com.e101.carry_porter.domain.user.entity;

import com.e101.carry_porter.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 2000)
    private String refreshToken;

    public static User createUser(String username, String password) {
        return User.builder()
                .username(username)
                .password(password)
                .build();
    }

    public void updateUsername(String username) {
        this.username = username;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Builder
    private User(String username, String password, String refreshToken) {
        this.username = username;
        this.password = password;
        this.refreshToken = refreshToken;
    }
}
