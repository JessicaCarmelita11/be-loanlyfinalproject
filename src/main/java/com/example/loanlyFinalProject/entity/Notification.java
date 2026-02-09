package com.example.loanlyFinalProject.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnore
  private User user;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, length = 500)
  private String message;

  @Column(name = "notification_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private NotificationType type;

  @Column(name = "reference_id")
  private Long referenceId;

  @Column(name = "is_read")
  @Builder.Default
  private Boolean isRead = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  public enum NotificationType {
    LOAN_SUBMITTED,
    LOAN_REVIEWED,
    LOAN_APPROVED,
    LOAN_REJECTED,
    LOAN_DISBURSED,
    SYSTEM
  }
}
