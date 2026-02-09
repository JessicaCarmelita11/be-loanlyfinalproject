package com.example.loanlyFinalProject.dto.response;

import java.util.Set;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

  private String accessToken;
  private String tokenType;
  private Long expiresIn;
  private UserInfo user;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class UserInfo {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Set<String> roles;
  }
}
