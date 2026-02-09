package com.example.loanlyFinalProject.controller;

import com.example.loanlyFinalProject.dto.request.ForgotPasswordRequest;
import com.example.loanlyFinalProject.dto.request.GoogleLoginRequest;
import com.example.loanlyFinalProject.dto.request.LoginRequest;
import com.example.loanlyFinalProject.dto.request.RegisterRequest;
import com.example.loanlyFinalProject.dto.request.ResetPasswordRequest;
import com.example.loanlyFinalProject.dto.response.ApiResponse;
import com.example.loanlyFinalProject.dto.response.AuthResponse;
import com.example.loanlyFinalProject.security.JwtService;
import com.example.loanlyFinalProject.service.AuthService;
import com.example.loanlyFinalProject.service.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

  private final AuthService authService;
  private final JwtService jwtService;
  private final TokenBlacklistService tokenBlacklistService;

  @PostMapping("/register")
  @Operation(
      summary = "Register a new user",
      description = "Creates a new user account with CUSTOMER role")
  public ResponseEntity<ApiResponse<AuthResponse>> register(
      @Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("User registered successfully", response));
  }

  @PostMapping("/login")
  @Operation(summary = "User login", description = "Authenticate user and return JWT token")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(ApiResponse.success("Login successful", response));
  }

  @PostMapping("/google-login")
  @Operation(
      summary = "Google Login",
      description = "Authenticate user using Firebase ID Token. Creates user if not exists.")
  public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
      @Valid @RequestBody GoogleLoginRequest request) {
    AuthResponse response = authService.googleLogin(request);
    return ResponseEntity.ok(ApiResponse.success("Google login successful", response));
  }

  @PostMapping("/logout")
  @Operation(summary = "User logout", description = "Invalidate the current JWT token")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<ApiResponse<Object>> logout(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      long remainingTime = jwtService.getRemainingExpirationTime(token);
      if (remainingTime > 0) {
        tokenBlacklistService.blacklistToken(token, remainingTime);
      }
    }
    return ResponseEntity.ok(ApiResponse.success("Logout successful"));
  }

  @PostMapping("/forgot-password")
  @Operation(
      summary = "Request password reset",
      description = "Send password reset link to user's email")
  public ResponseEntity<ApiResponse<Object>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {
    authService.forgotPassword(request);
    return ResponseEntity.ok(
        ApiResponse.success("Password reset link has been sent to your email"));
  }

  @PostMapping("/reset-password")
  @Operation(summary = "Reset password", description = "Reset password using the token from email")
  public ResponseEntity<ApiResponse<Object>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully"));
  }

  @GetMapping("/validate-token")
  @Operation(
      summary = "Validate reset token",
      description = "Check if a password reset token is valid")
  public ResponseEntity<ApiResponse<Boolean>> validateResetToken(@RequestParam String token) {
    boolean isValid = authService.validateResetToken(token);
    return ResponseEntity.ok(ApiResponse.success("Token validation result", isValid));
  }
}
