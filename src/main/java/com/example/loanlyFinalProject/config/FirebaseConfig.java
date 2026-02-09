package com.example.loanlyFinalProject.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class FirebaseConfig {

  @PostConstruct
  public void initialize() {
    try {
      InputStream serviceAccountStream = null;
      File file = new File("firebase-service-account.json");

      if (file.exists()) {
        System.out.println("Loading Firebase config from file system: " + file.getAbsolutePath());
        serviceAccountStream = new FileInputStream(file);
      } else {
        System.out.println("Loading Firebase config from classpath");
        ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
        if (resource.exists()) {
          serviceAccountStream = resource.getInputStream();
        }
      }

      if (serviceAccountStream != null) {
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
            "Firebase service account file not found (checked file system and classpath). Push notifications will not work.");
      }
    } catch (IOException e) {
      System.err.println("Error initializing Firebase: " + e.getMessage());
    }
  }

  @org.springframework.context.annotation.Bean
  @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
      name = "firebase.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public com.google.firebase.auth.FirebaseAuth firebaseAuth() {
    if (FirebaseApp.getApps().isEmpty()) {
      return null;
    }
    return com.google.firebase.auth.FirebaseAuth.getInstance();
  }
}
