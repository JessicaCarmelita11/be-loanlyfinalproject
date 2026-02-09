package com.example.loanlyFinalProject.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

  @NotBlank(message = "Username or email is required")
  private String usernameOrEmail;

  @NotBlank(message = "Password is required")
  private String password;

  // Optional FCM token from mobile device
  private String fcmToken;
}
