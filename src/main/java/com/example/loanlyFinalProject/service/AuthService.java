package com.example.loanlyFinalProject.service;

import com.example.loanlyFinalProject.dto.request.ForgotPasswordRequest;
import com.example.loanlyFinalProject.dto.request.GoogleLoginRequest;
import com.example.loanlyFinalProject.dto.request.LoginRequest;
import com.example.loanlyFinalProject.dto.request.RegisterRequest;
import com.example.loanlyFinalProject.dto.request.ResetPasswordRequest;
import com.example.loanlyFinalProject.dto.response.AuthResponse;
import com.example.loanlyFinalProject.entity.PasswordResetToken;
import com.example.loanlyFinalProject.entity.Role;
import com.example.loanlyFinalProject.entity.User;
import com.example.loanlyFinalProject.exception.DuplicateResourceException;
import com.example.loanlyFinalProject.exception.InvalidTokenException;
import com.example.loanlyFinalProject.exception.ResourceNotFoundException;
import com.example.loanlyFinalProject.repository.PasswordResetTokenRepository;
import com.example.loanlyFinalProject.repository.RoleRepository;
import com.example.loanlyFinalProject.repository.UserRepository;
import com.example.loanlyFinalProject.security.CustomUserDetails;
import com.example.loanlyFinalProject.security.JwtService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final EmailService emailService;

  @Autowired(required = false)
  private FirebaseAuth firebaseAuth;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    // Check if username already exists
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new DuplicateResourceException("Username already exists");
    }

    // Check if email already exists
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new DuplicateResourceException("Email already exists");
    }

    // Get default role (CUSTOMER)
    Role customerRole =
        roleRepository
            .findByName("CUSTOMER")
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Default role CUSTOMER not found. Please run data initializer."));

    // Create new user
    User user =
        User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .phone(request.getPhone())
            .isActive(true)
            .build();

    user.getRoles().add(customerRole);

    User savedUser = userRepository.save(user);
    log.info("User registered successfully: {}", savedUser.getUsername());

    // Send welcome email (async, non-blocking)
    emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getUsername());

    // Generate JWT token
    CustomUserDetails userDetails = new CustomUserDetails(savedUser);
    String token = jwtService.generateToken(userDetails);

    return buildAuthResponse(token, savedUser);
  }

  public AuthResponse login(LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsernameOrEmail(), request.getPassword()));

    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    User user = userDetails.getUser();

    // Save FCM token if provided
    if (request.getFcmToken() != null && !request.getFcmToken().isEmpty()) {
      user.setFcmToken(request.getFcmToken());
      userRepository.save(user);
    }

    String token = jwtService.generateToken(authentication);

    log.info("User logged in successfully: {}", userDetails.getUsername());

    return buildAuthResponse(token, user);
  }

  @Transactional
  public void forgotPassword(ForgotPasswordRequest request) {
    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "User not found with email: " + request.getEmail()));

    // Generate reset token
    String token = UUID.randomUUID().toString();

    PasswordResetToken resetToken =
        PasswordResetToken.builder()
            .user(user)
            .token(token)
            .expiryDate(LocalDateTime.now().plusHours(1)) // Token expires in 1 hour
            .isUsed(false)
            .build();

    passwordResetTokenRepository.save(resetToken);

    // Send reset email
    emailService.sendPasswordResetEmail(user.getEmail(), token);

    log.info("Password reset token generated for user: {}", user.getUsername());
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    PasswordResetToken resetToken =
        passwordResetTokenRepository
            .findByTokenAndIsUsedFalse(request.getToken())
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

    if (resetToken.isExpired()) {
      throw new InvalidTokenException("Reset token has expired");
    }

    User user = resetToken.getUser();
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    // Mark token as used
    resetToken.setIsUsed(true);
    passwordResetTokenRepository.save(resetToken);

    log.info("Password reset successfully for user: {}", user.getUsername());
  }

  public boolean validateResetToken(String token) {
    return passwordResetTokenRepository
        .findByTokenAndIsUsedFalse(token)
        .map(resetToken -> !resetToken.isExpired())
        .orElse(false);
  }

  @Transactional
  public AuthResponse googleLogin(GoogleLoginRequest request) {
    try {
      // 1. Verify Firebase ID Token
      if (firebaseAuth == null) {
        throw new InvalidTokenException("Firebase is not configured. Google Login is unavailable.");
      }
      FirebaseToken decodedToken = firebaseAuth.verifyIdToken(request.getFirebaseToken());
      String email = decodedToken.getEmail();
      String name = decodedToken.getName();

      if (email == null || email.isEmpty()) {
        throw new InvalidTokenException("Email is required in Firebase Token");
      }

      // 2. Check if user exists
      User user = userRepository.findByEmail(email).orElse(null);

      if (user == null) {
        // 3. Create new user if not exists
        Role customerRole =
            roleRepository
                .findByName("CUSTOMER")
                .orElseThrow(
                    () -> new ResourceNotFoundException("Default role CUSTOMER not found"));

        String randomPassword = UUID.randomUUID().toString(); // Random password for OAuth users

        user =
            User.builder()
                .username(email.split("@")[0]) // Use email prefix as username
                .email(email)
                .fullName(name != null ? name : email.split("@")[0])
                .password(passwordEncoder.encode(randomPassword))
                .isActive(true) // Auto activate
                .build();

        user.getRoles().add(customerRole);

        // Handle duplicate username edge case
        int suffix = 1;
        String baseUsername = user.getUsername();
        while (userRepository.existsByUsername(user.getUsername())) {
          user.setUsername(baseUsername + suffix);
          suffix++;
        }

        user = userRepository.save(user);
        log.info("New user registered via Google Login: {}", user.getUsername());

        // Send welcome email (async)
        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
      }

      // 4. Update FCM Token if provided
      if (request.getFcmToken() != null && !request.getFcmToken().isEmpty()) {
        user.setFcmToken(request.getFcmToken());
        userRepository.save(user);
      }

      // 5. Generate JWT Token
      CustomUserDetails userDetails = new CustomUserDetails(user);
      String token = jwtService.generateToken(userDetails);

      log.info("User logged in via Google: {}", user.getUsername());

      return buildAuthResponse(token, user);

    } catch (Exception e) {
      log.error("Google Login Failed", e);
      throw new InvalidTokenException("Invalid Firebase Token: " + e.getMessage());
    }
  }

  private AuthResponse buildAuthResponse(String token, User user) {
    Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

    return AuthResponse.builder()
        .accessToken(token)
        .tokenType("Bearer")
        .expiresIn(jwtService.getExpirationTime())
        .user(
            AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build())
        .build();
  }
}
