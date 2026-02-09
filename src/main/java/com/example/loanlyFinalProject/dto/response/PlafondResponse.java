package com.example.loanlyFinalProject.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlafondResponse {

  private Long id;
  private String name;
  private String description;
  private BigDecimal maxAmount;
  private Boolean isActive;
  private LocalDateTime createdAt;
}
