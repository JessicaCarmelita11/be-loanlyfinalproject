package com.example.loanlyFinalProject.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisbursementResponse {

  private Long id;
  private Long userPlafondId;
  private String plafondName;
  private String customerUsername;
  private String customerName;
  private String bankName;
  private String accountNumber;

  // Amount details
  private BigDecimal amount;
  private BigDecimal interestRate;
  private Integer tenorMonth;
  private BigDecimal interestAmount;
  private BigDecimal totalAmount;

  // Status
  private String status;
  private LocalDateTime requestedAt;
  private LocalDateTime disbursedAt;
  private String disbursedByUsername;
  private String note;

  // Location Data
  private BigDecimal requestLatitude;
  private BigDecimal requestLongitude;

  // Remaining limit after disbursement
  private BigDecimal remainingLimit;
}
