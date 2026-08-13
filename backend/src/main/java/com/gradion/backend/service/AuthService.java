package com.gradion.backend.service;

import com.gradion.backend.config.JwtUtil;
import com.gradion.backend.dto.LoginResponse;
import com.gradion.backend.model.User;
import com.gradion.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public LoginResponse loginOrRegister(String name, String email) {

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .name(name)
                            .email(email)
                            .build();
                    return userRepository.save(newUser);
                });

        String token = jwtUtil.generateToken(user);
        return LoginResponse.builder()
                .token(token)
                .user(user)
                .build();
    }
}
