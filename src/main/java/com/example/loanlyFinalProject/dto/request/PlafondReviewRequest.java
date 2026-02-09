package com.example.loanlyFinalProject.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlafondReviewRequest {

  @NotNull(message = "Application ID is required")
  private Long applicationId;

  @NotNull(message = "Approved flag is required")
  private Boolean approved;

  @DecimalMin(value = "0", inclusive = false, message = "Approved limit must be positive")
  private BigDecimal approvedLimit; // Only for approval, not rejection

  private String note;
}
