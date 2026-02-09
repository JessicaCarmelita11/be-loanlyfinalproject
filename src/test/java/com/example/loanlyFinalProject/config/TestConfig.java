package com.example.loanlyFinalProject.config;

import com.google.firebase.auth.FirebaseAuth;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestConfig {

  @Bean
  @Primary
  public FirebaseAuth firebaseAuth() {
    return Mockito.mock(FirebaseAuth.class);
  }
}
