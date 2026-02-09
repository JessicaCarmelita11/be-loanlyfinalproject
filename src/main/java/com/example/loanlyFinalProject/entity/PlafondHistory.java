package com.example.loanlyFinalProject.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "plafond_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlafondHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_plafond_id", nullable = false)
  @JsonIgnore
  private UserPlafond userPlafond;

  @Column(name = "previous_status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private UserPlafond.PlafondApplicationStatus previousStatus;

  @Column(name = "new_status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private UserPlafond.PlafondApplicationStatus newStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "action_by_user_id", nullable = false)
  @JsonIgnore
  private User actionByUser;

  @Column(name = "action_by_role", nullable = false, length = 50)
  private String actionByRole;

  @Column(length = 255)
  private String note;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
