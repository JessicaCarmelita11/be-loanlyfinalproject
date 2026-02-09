package com.example.loanlyFinalProject.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for credit eligibility check. Used by frontend to determine if user can apply for
 * new plafond.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditEligibilityResponse {

  private boolean canApply;
  private String reason;
  private String reasonCode;

  // Current active limit info (if exists)
  private ActiveLimitInfo currentLimit;

  // Minimum tier required for next application
  private PlafondResponse minimumNextTier;

  // List of plafonds eligible for tier-up
  private List<PlafondResponse> eligiblePlafonds;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ActiveLimitInfo {
    private Long applicationId;
    private String plafondName;
    private Long plafondId;
    private BigDecimal maxAmount;
    private BigDecimal approvedLimit;
    private BigDecimal usedAmount;
    private BigDecimal remainingLimit;
  }

  // Reason codes
  public static final String REASON_ELIGIBLE = "ELIGIBLE";
  public static final String REASON_ACTIVE_LIMIT_EXISTS = "ACTIVE_LIMIT_EXISTS";
  public static final String REASON_PENDING_APPLICATION = "PENDING_APPLICATION";
  public static final String REASON_NO_HIGHER_TIER = "NO_HIGHER_TIER_AVAILABLE";
}
