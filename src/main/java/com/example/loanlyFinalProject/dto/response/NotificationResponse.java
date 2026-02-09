package com.example.loanlyFinalProject.dto.response;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

  private Long id;
  private String title;
  private String message;
  private String type;
  private Long referenceId;
  private Boolean isRead;
  private LocalDateTime createdAt;
  private LocalDateTime readAt;
}
