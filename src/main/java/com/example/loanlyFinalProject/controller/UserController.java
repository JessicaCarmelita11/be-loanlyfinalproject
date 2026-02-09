package com.example.loanlyFinalProject.controller;

import com.example.loanlyFinalProject.dto.response.ApiResponse;
import com.example.loanlyFinalProject.entity.Role;
import com.example.loanlyFinalProject.entity.User;
import com.example.loanlyFinalProject.repository.RoleRepository;
import com.example.loanlyFinalProject.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Admin endpoints for user management")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  // ========== ROLES ENDPOINTS ==========

  @GetMapping("/roles")
  @Operation(summary = "Get all roles (Admin)", description = "Returns list of all available roles")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
  public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
    List<RoleResponse> roles =
        roleRepository.findAll().stream()
            .map(
                role ->
                    RoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .description(role.getDescription())
                        .build())
            .collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roles));
  }

  // ========== USERS ENDPOINTS ==========

  @GetMapping("/users")
  @Operation(summary = "Get all users (Admin)", description = "Returns list of all users")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
  public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
    List<UserResponse> users =
        userRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
  }

  @GetMapping("/users/{id}")
  @Operation(summary = "Get user by ID (Admin)", description = "Returns user details by ID")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
  public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
    User user =
        userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    return ResponseEntity.ok(
        ApiResponse.success("User retrieved successfully", mapToResponse(user)));
  }

  @PostMapping("/users")
  @Operation(
      summary = "Create new user (Admin)",
      description = "Creates a new user with specified roles")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<UserResponse>> createUser(
      @Valid @RequestBody CreateUserRequest request) {
    // Check if username already exists
    if (userRepository.existsByUsername(request.getUsername())) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Username already exists"));
    }

    // Check if email already exists
    if (userRepository.existsByEmail(request.getEmail())) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Email already exists"));
    }

    // Get roles - support both roleIds and role names
    Set<Role> roles = new HashSet<>();
    if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
      // Support roleIds (from frontend)
      roles.addAll(roleRepository.findAllById(request.getRoleIds()));
    } else if (request.getRoles() != null && !request.getRoles().isEmpty()) {
      // Support role names
      for (String roleName : request.getRoles()) {
        roleRepository.findByName(roleName).ifPresent(roles::add);
      }
    }

    // Create user
    User user =
        User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .phone(request.getPhone())
            .isActive(true)
            .roles(roles)
            .build();

    User savedUser = userRepository.save(user);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("User created successfully", mapToResponse(savedUser)));
  }

  @PutMapping("/users/{id}")
  @Operation(summary = "Update user (Admin)", description = "Updates an existing user")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<UserResponse>> updateUser(
      @PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {

    User user =
        userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

    // Update fields
    if (request.getFullName() != null) {
      user.setFullName(request.getFullName());
    }
    if (request.getEmail() != null) {
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
    if (request.getIsActive() != null) {
      user.setIsActive(request.getIsActive());
    }
    if (request.getPassword() != null && !request.getPassword().isEmpty()) {
      user.setPassword(passwordEncoder.encode(request.getPassword()));
    }

    // Update roles if provided - support both roleIds and role names
    // If roleIds is provided (even empty), always update roles
    if (request.getRoleIds() != null) {
      Set<Role> roles = new HashSet<>();
      if (!request.getRoleIds().isEmpty()) {
        roles.addAll(roleRepository.findAllById(request.getRoleIds()));
      }
      user.setRoles(roles);
    } else if (request.getRoles() != null) {
      Set<Role> roles = new HashSet<>();
      if (!request.getRoles().isEmpty()) {
        for (String roleName : request.getRoles()) {
          roleRepository.findByName(roleName).ifPresent(roles::add);
        }
      }
      user.setRoles(roles);
    }

    User updatedUser = userRepository.save(user);

    return ResponseEntity.ok(
        ApiResponse.success("User updated successfully", mapToResponse(updatedUser)));
  }

  @DeleteMapping("/users/{id}")
  @Operation(summary = "Delete user (Admin)", description = "Deletes a user")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
    User user =
        userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

    userRepository.delete(user);

    return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
  }

  @PutMapping("/users/{id}/toggle-status")
  @Operation(
      summary = "Toggle user status (Admin)",
      description = "Activates or deactivates a user")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable Long id) {
    User user =
        userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

    user.setIsActive(!user.getIsActive());
    User updatedUser = userRepository.save(user);

    String message =
        user.getIsActive() ? "User activated successfully" : "User deactivated successfully";
    return ResponseEntity.ok(ApiResponse.success(message, mapToResponse(updatedUser)));
  }

  private UserResponse mapToResponse(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .fullName(user.getFullName())
        .phone(user.getPhone())
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
  public static class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private Boolean isActive;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
  }

  @lombok.Getter
  @lombok.Setter
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  @lombok.Builder
  public static class RoleResponse {
    private Long id;
    private String name;
    private String description;
  }

  @lombok.Getter
  @lombok.Setter
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class CreateUserRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String phone;

    private List<String> roles; // Support role names
    private List<Long> roleIds; // Support role IDs (from frontend)
  }

  @lombok.Getter
  @lombok.Setter
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class UpdateUserRequest {
    private String fullName;

    @Email(message = "Email must be valid")
    private String email;

    private String password;
    private String phone;
    private Boolean isActive;
    private List<String> roles; // Support role names
    private List<Long> roleIds; // Support role IDs (from frontend)
  }
}
