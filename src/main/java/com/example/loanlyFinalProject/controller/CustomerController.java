package com.example.loanlyFinalProject.controller;

import com.example.loanlyFinalProject.dto.response.ApiResponse;
import com.example.loanlyFinalProject.entity.CustomerProfile;
import com.example.loanlyFinalProject.entity.Role;
import com.example.loanlyFinalProject.entity.User;
import com.example.loanlyFinalProject.repository.CustomerProfileRepository;
import com.example.loanlyFinalProject.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@Tag(name = "Customer Profile", description = "Customer profile management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerController {

  private final UserRepository userRepository;
  private final CustomerProfileRepository customerProfileRepository;
  private final PasswordEncoder passwordEncoder;

  /** Get current customer's profile */
  @GetMapping("/profile")
  @Operation(
      summary = "Get customer profile",
      description = "Returns the authenticated customer's profile")
  public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile() {
    User user = getCurrentUser();
    if (user == null) {
      return ResponseEntity.status(401).body(ApiResponse.error("User not authenticated"));
    }

    // Get or create CustomerProfile
    CustomerProfile profile = getOrCreateCustomerProfile(user);

    return ResponseEntity.ok(
        ApiResponse.success("Profile retrieved successfully", mapToProfileResponse(user, profile)));
  }

  /** Update current customer's profile */
  @PutMapping("/profile")
  @Operation(
      summary = "Update customer profile",
      description = "Updates the authenticated customer's profile")
  public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateProfile(
      @Valid @RequestBody UpdateProfileRequest request) {

    User user = getCurrentUser();
    if (user == null) {
      return ResponseEntity.status(401).body(ApiResponse.error("User not authenticated"));
    }

    // Update User fields if provided
    if (request.getFullName() != null && !request.getFullName().isEmpty()) {
      user.setFullName(request.getFullName());
    }
    if (request.getEmail() != null && !request.getEmail().isEmpty()) {
      // Check if email is being changed and if new email exists
      if (!user.getEmail().equals(request.getEmail())
          && userRepository.existsByEmail(request.getEmail())) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Email already exists"));
      }
      user.setEmail(request.getEmail());
    }
    if (request.getPhone() != null) {
      user.setPhone(request.getPhone());
    }

    User updatedUser = userRepository.save(user);

    // Update CustomerProfile fields
    CustomerProfile profile = getOrCreateCustomerProfile(user);

    if (request.getAddress() != null) {
      profile.setAddress(request.getAddress());
    }
    if (request.getDateOfBirth() != null) {
      profile.setDateOfBirth(request.getDateOfBirth());
    }

    CustomerProfile updatedProfile = customerProfileRepository.save(profile);

    return ResponseEntity.ok(
        ApiResponse.success(
            "Profile updated successfully", mapToProfileResponse(updatedUser, updatedProfile)));
  }

  /** Change current customer's password */
  @PostMapping("/change-password")
  @Operation(
      summary = "Change password",
      description = "Changes the authenticated customer's password")
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

  private CustomerProfile getOrCreateCustomerProfile(User user) {
    return customerProfileRepository
        .findByUserId(user.getId())
        .orElseGet(
            () -> {
              // Auto-create CustomerProfile for the user
              CustomerProfile newProfile = CustomerProfile.builder().user(user).build();
              return customerProfileRepository.save(newProfile);
            });
  }

  private CustomerProfileResponse mapToProfileResponse(User user, CustomerProfile profile) {
    return CustomerProfileResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .fullName(user.getFullName())
        .phone(user.getPhone())
        .address(profile != null ? profile.getAddress() : null)
        .dateOfBirth(profile != null ? profile.getDateOfBirth() : null)
        .isActive(user.getIsActive())
        .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }

  // ========== REQUEST/RESPONSE DTOs ==========

  @lombok.Getter
  @lombok.Setter
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  @lombok.Builder
  public static class CustomerProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private LocalDate dateOfBirth;
    private Boolean isActive;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
  }

  @lombok.Getter
  @lombok.Setter
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class UpdateProfileRequest {
    private String fullName;

    @Email(message = "Email must be valid")
    private String email;

    private String phone;
    private String address;
    private LocalDate dateOfBirth;
  }

  @lombok.Getter
  @lombok.Setter
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class ChangePasswordRequest {
    @jakarta.validation.constraints.NotBlank(message = "Current password is required")
    private String currentPassword;

    @jakarta.validation.constraints.NotBlank(message = "New password is required")
    @jakarta.validation.constraints.Size(
        min = 6,
        message = "New password must be at least 6 characters")
    private String newPassword;
  }
}
