package com.example.loanlyFinalProject.controller;

import com.example.loanlyFinalProject.dto.request.ChangePasswordRequest;
import com.example.loanlyFinalProject.dto.response.ApiResponse;
import com.example.loanlyFinalProject.entity.User;
import com.example.loanlyFinalProject.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Generic profile management endpoints for all roles")
@SecurityRequirement(name = "Bearer Authentication")
public class ProfileController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @PostMapping("/change-password")
  @Operation(summary = "Change password", description = "Changes the authenticated user's password")
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @Valid @RequestBody ChangePasswordRequest request) {

    User user = getCurrentUser();
    if (user == null) {
      return ResponseEntity.status(401).body(ApiResponse.error("User not authenticated"));
    }

    // Verify current password
    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Current password is incorrect"));
    }

    // Check if new password is same as old
    if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("New password cannot be the same as current password"));
    }

    // Update password
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
  }

  private User getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
      return null;
    }
    String username = auth.getName();
    return userRepository.findByUsername(username).orElse(null);
  }
}
