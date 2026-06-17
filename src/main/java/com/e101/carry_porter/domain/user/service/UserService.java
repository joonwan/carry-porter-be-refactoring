package com.e101.carry_porter.domain.user.service;

import com.e101.carry_porter.domain.user.entity.User;
import com.e101.carry_porter.domain.user.exception.UserErrorCode;
import com.e101.carry_porter.domain.user.exception.UserException;
import com.e101.carry_porter.domain.user.repository.UserRepository;
import com.e101.carry_porter.domain.user.service.dto.request.CreateUserServiceRequest;
import com.e101.carry_porter.domain.user.service.dto.response.CreateUserServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CreateUserServiceResponse createUser(CreateUserServiceRequest request) {
        validateDuplicateUsername(request.username());

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.createUser(request.username(), encodedPassword);
        User savedUser = userRepository.save(user);

        return CreateUserServiceResponse.from(savedUser);
    }

    private void validateDuplicateUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new UserException(UserErrorCode.USERNAME_ALREADY_EXISTS);
        }
    }
}
