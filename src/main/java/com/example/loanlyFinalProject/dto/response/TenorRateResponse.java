package com.example.loanlyFinalProject.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenorRateResponse {
  private Long id;
  private Long plafondId;
  private String plafondName;
  private Integer tenorMonth;
  private BigDecimal interestRate;
  private String description;
  private Boolean isActive;
}
