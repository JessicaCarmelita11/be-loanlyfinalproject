package com.example.loanlyFinalProject.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.loanlyFinalProject.dto.request.LoginRequest;
import com.example.loanlyFinalProject.dto.request.RegisterRequest;
import com.example.loanlyFinalProject.dto.response.AuthResponse;
import com.example.loanlyFinalProject.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private AuthService authService;

  @Test
  @DisplayName("Register - Should return 200 with token")
  void register_ShouldReturnToken_WhenValidRequest() throws Exception {
    // Arrange
    RegisterRequest request =
        RegisterRequest.builder()
            .username("newuser")
            .email("new@example.com")
            .password("Password123!")
            .fullName("New User")
            .build();

    AuthResponse response =
        AuthResponse.builder().accessToken("jwt-token").tokenType("Bearer").build();

    when(authService.register(any(RegisterRequest.class))).thenReturn(response);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accessToken").value("jwt-token"));
  }

  @Test
  @DisplayName("Register - Should return 400 when validation fails")
  void register_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
    // Arrange - missing required fields
    RegisterRequest request =
        RegisterRequest.builder().username("").email("invalid-email").password("123").build();

    // Act & Assert
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Login - Should return 200 with token")
  void login_ShouldReturnToken_WhenValidCredentials() throws Exception {
    // Arrange
    LoginRequest request =
        LoginRequest.builder().usernameOrEmail("testuser").password("Password123!").build();

    AuthResponse response =
        AuthResponse.builder().accessToken("jwt-token").tokenType("Bearer").build();

    when(authService.login(any(LoginRequest.class))).thenReturn(response);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accessToken").value("jwt-token"));
  }
}
