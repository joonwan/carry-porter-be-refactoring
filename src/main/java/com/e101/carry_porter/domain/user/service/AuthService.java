package com.e101.carry_porter.domain.user.service;

import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.domain.user.exception.UserErrorCode;
import com.e101.carry_porter.domain.user.exception.UserException;
import com.e101.carry_porter.domain.user.repository.UserRepository;
import com.e101.carry_porter.domain.user.service.dto.request.LoginServiceRequest;
import com.e101.carry_porter.domain.user.service.dto.response.LoginServiceResponse;
import com.e101.carry_porter.global.security.JwtToken;
import com.e101.carry_porter.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginServiceResponse login(LoginServiceRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UserException(UserErrorCode.LOGIN_FAILED));

        validatePassword(request.password(), user.getPassword());

        JwtToken jwtToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername());

        return LoginServiceResponse.of(jwtToken.accessToken(), jwtToken.expiresAt());
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new UserException(UserErrorCode.LOGIN_FAILED);
        }
    }
}
