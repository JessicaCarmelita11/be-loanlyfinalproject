package com.example.loanlyFinalProject.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class FirebaseConfig {

  @PostConstruct
  public void initialize() {
    try {
      ClassPathResource serviceAccount = new ClassPathResource("firebase-service-account.json");

      if (serviceAccount.exists()) {
        InputStream serviceAccountStream = serviceAccount.getInputStream();

        FirebaseOptions options =
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
          FirebaseApp.initializeApp(options);
          System.out.println("Firebase application has been initialized");
        }
      } else {
        System.out.println(
            "Firebase service account file not found. Push notifications will not work.");
      }
    } catch (IOException e) {
      System.err.println("Error initializing Firebase: " + e.getMessage());
    }
  }

  @org.springframework.context.annotation.Bean
  public com.google.firebase.auth.FirebaseAuth firebaseAuth() {
    return com.google.firebase.auth.FirebaseAuth.getInstance();
  }
}
