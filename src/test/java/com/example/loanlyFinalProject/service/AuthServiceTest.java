package com.example.loanlyFinalProject.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.example.loanlyFinalProject.dto.request.LoginRequest;
import com.example.loanlyFinalProject.dto.request.RegisterRequest;
import com.example.loanlyFinalProject.dto.response.AuthResponse;
import com.example.loanlyFinalProject.entity.Role;
import com.example.loanlyFinalProject.entity.User;
import com.example.loanlyFinalProject.exception.DuplicateResourceException;
import com.example.loanlyFinalProject.repository.PasswordResetTokenRepository;
import com.example.loanlyFinalProject.repository.RoleRepository;
import com.example.loanlyFinalProject.repository.UserRepository;
import com.example.loanlyFinalProject.security.CustomUserDetails;
import com.example.loanlyFinalProject.security.JwtService;
import com.google.firebase.auth.FirebaseAuth;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private RoleRepository roleRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private JwtService jwtService;

  @Mock private AuthenticationManager authenticationManager;

  @Mock private EmailService emailService;

  @Mock private FirebaseAuth firebaseAuth;

  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;

  @InjectMocks private AuthService authService;

  private User testUser;
  private Role customerRole;

  @BeforeEach
  void setUp() {
    customerRole = Role.builder().id(1L).name("CUSTOMER").permissions(new HashSet<>()).build();

    testUser =
        User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("encodedPassword")
            .fullName("Test User")
            .roles(Set.of(customerRole))
            .isActive(true)
            .build();
  }

  @Test
  @DisplayName("Register - Should register new user successfully")
  void register_ShouldRegisterNewUser_WhenValidRequest() {
    // Arrange
    RegisterRequest request =
        RegisterRequest.builder()
            .username("newuser")
            .email("new@example.com")
            .password("password123")
            .fullName("New User")
            .build();

    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("jwt-token");

    // Act
    AuthResponse response = authService.register(request);

    // Assert
    assertNotNull(response);
    assertEquals("jwt-token", response.getAccessToken());
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("Register - Should throw exception when username exists")
  void register_ShouldThrowException_WhenUsernameExists() {
    // Arrange
    RegisterRequest request =
        RegisterRequest.builder()
            .username("existinguser")
            .email("new@example.com")
            .password("password123")
            .fullName("New User")
            .build();

    when(userRepository.existsByUsername("existinguser")).thenReturn(true);

    // Act & Assert
    assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Register - Should throw exception when email exists")
  void register_ShouldThrowException_WhenEmailExists() {
    // Arrange
    RegisterRequest request =
        RegisterRequest.builder()
            .username("newuser")
            .email("existing@example.com")
            .password("password123")
            .fullName("New User")
            .build();

    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

    // Act & Assert
    assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Login - Should login successfully with valid credentials")
  void login_ShouldLoginSuccessfully_WhenValidCredentials() {
    // Arrange
    LoginRequest request =
        LoginRequest.builder().usernameOrEmail("testuser").password("password123").build();

    CustomUserDetails userDetails = new CustomUserDetails(testUser);
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(userDetails);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(jwtService.generateToken(any(Authentication.class))).thenReturn("jwt-token");
    when(jwtService.getExpirationTime()).thenReturn(86400000L);

    // Act
    AuthResponse response = authService.login(request);

    // Assert
    assertNotNull(response);
    assertEquals("jwt-token", response.getAccessToken());
    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  @DisplayName("Login - Should throw exception when authentication fails")
  void login_ShouldThrowException_WhenUserNotFound() {
    // Arrange
    LoginRequest request =
        LoginRequest.builder().usernameOrEmail("nonexistent").password("password123").build();

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Invalid credentials"));

    // Act & Assert
    assertThrows(BadCredentialsException.class, () -> authService.login(request));
  }
}
