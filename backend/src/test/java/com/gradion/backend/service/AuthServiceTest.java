package com.gradion.backend.service;

import com.gradion.backend.config.JwtUtil;
import com.gradion.backend.dto.LoginResponse;
import com.gradion.backend.model.User;
import com.gradion.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .name("Existing User")
                .email("existing@example.com")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void loginOrRegister_withNewEmail_createsNewUserAndReturnsToken() {
        // Given
        String newName = "New User";
        String newEmail = "new@example.com";
        String expectedToken = "mocked_new_token";

        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L); // Simulate ID being set after save
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });
        when(jwtUtil.generateToken(any(User.class))).thenReturn(expectedToken);

        // When
        LoginResponse response = authService.loginOrRegister(newName, newEmail);

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isEmpty());
        assertEquals(expectedToken, response.getToken());

        assertNotNull(response.getUser());
        assertEquals(newName, response.getUser().getName());
        assertEquals(newEmail, response.getUser().getEmail());
        assertNotNull(response.getUser().getId()); // Ensure ID was set
        assertNotNull(response.getUser().getCreatedAt());

        verify(userRepository, times(1)).findByEmail(newEmail);
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtUtil, times(1)).generateToken(any(User.class));
    }

    @Test
    void loginOrRegister_withExistingEmail_loadsExistingUserAndReturnsToken() {
        // Given
        String expectedToken = "mocked_existing_token";

        when(userRepository.findByEmail(existingUser.getEmail())).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateToken(existingUser)).thenReturn(expectedToken);

        // When
        LoginResponse response = authService.loginOrRegister(existingUser.getName(), existingUser.getEmail());

        // Then
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isEmpty());
        assertEquals(expectedToken, response.getToken());

        assertNotNull(response.getUser());
        assertEquals(existingUser.getId(), response.getUser().getId());
        assertEquals(existingUser.getName(), response.getUser().getName());
        assertEquals(existingUser.getEmail(), response.getUser().getEmail());

        verify(userRepository, times(1)).findByEmail(existingUser.getEmail());
        verify(userRepository, never()).save(any(User.class)); // Should not save existing user
        verify(jwtUtil, times(1)).generateToken(existingUser);
    }

    @Test
    void loginOrRegister_withNullEmail_throwsIllegalArgumentException() {
        // Given
        String name = "Test User";
        String email = null;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> authService.loginOrRegister(name, email));

        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtUtil, never()).generateToken(any(User.class));
    }

    @Test
    void loginOrRegister_returnedTokenIsNotNullAndNotEmpty() {
        // This is implicitly tested in the first two test cases, but adding an explicit check for clarity.
        // Given
        String newName = "Token Check User";
        String newEmail = "token@example.com";
        String expectedToken = "non_null_non_empty_token";

        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(3L);
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });
        when(jwtUtil.generateToken(any(User.class))).thenReturn(expectedToken);

        // When
        LoginResponse response = authService.loginOrRegister(newName, newEmail);

        // Then
        assertNotNull(response.getToken(), "Token should not be null");
        assertFalse(response.getToken().isEmpty(), "Token should not be empty");
        assertEquals(expectedToken, response.getToken());
    }
}
