package com.example.loanlyFinalProject.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenorRateRequest {

  @NotNull(message = "Plafond ID is required")
  private Long plafondId;

  @NotNull(message = "Tenor month is required")
  @Positive(message = "Tenor must be positive")
  private Integer tenorMonth;

  @NotNull(message = "Interest rate is required")
  private BigDecimal interestRate;

  private String description;
}
