package com.example.loanlyFinalProject.service;

import com.example.loanlyFinalProject.dto.response.CreditEligibilityResponse;
import com.example.loanlyFinalProject.dto.response.CreditEligibilityResponse.ActiveLimitInfo;
import com.example.loanlyFinalProject.dto.response.PlafondResponse;
import com.example.loanlyFinalProject.entity.Plafond;
import com.example.loanlyFinalProject.entity.UserPlafond;
import com.example.loanlyFinalProject.repository.PlafondRepository;
import com.example.loanlyFinalProject.repository.UserPlafondRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for checking customer credit eligibility and tier-up requirements. Implements the
 * Continuous Credit Lifecycle business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditEligibilityService {

  private final UserPlafondRepository userPlafondRepository;
  private final PlafondRepository plafondRepository;

  /**
   * Check if user is eligible to apply for a new plafond. Rules: 1. No pending applications 2. No
   * active limit with remaining balance > 0 3. If has previous plafond, must apply for higher tier
   */
  public CreditEligibilityResponse checkEligibility(Long userId) {
    log.info("Checking credit eligibility for user: {}", userId);

    // Rule 1: Check for pending applications
    if (userPlafondRepository.hasPendingApplication(userId)) {
      log.info("User {} has pending application", userId);
      return CreditEligibilityResponse.builder()
          .canApply(false)
          .reason("Anda memiliki pengajuan yang masih dalam proses review")
          .reasonCode(CreditEligibilityResponse.REASON_PENDING_APPLICATION)
          .eligiblePlafonds(Collections.emptyList())
          .build();
    }

    // Rule 2: Check for active limit with remaining balance
    List<UserPlafond> activeWithBalance =
        userPlafondRepository.findActiveWithRemainingLimit(userId);
    if (!activeWithBalance.isEmpty()) {
      UserPlafond active = activeWithBalance.get(0);
      BigDecimal remaining = active.getAvailableLimit();

      log.info("User {} has active limit with remaining: {}", userId, remaining);

      return CreditEligibilityResponse.builder()
          .canApply(false)
          .reason("Anda masih memiliki sisa limit aktif sebesar Rp " + formatCurrency(remaining))
          .reasonCode(CreditEligibilityResponse.REASON_ACTIVE_LIMIT_EXISTS)
          .currentLimit(mapToActiveLimitInfo(active))
          .eligiblePlafonds(Collections.emptyList())
          .build();
    }

    // Rule 3: Determine minimum tier for tier-up
    BigDecimal minimumAmount = getMinimumNextTierAmount(userId);
    List<PlafondResponse> eligiblePlafonds = getEligiblePlafonds(minimumAmount);

    // Check if there are higher tiers available
    if (eligiblePlafonds.isEmpty() && minimumAmount.compareTo(BigDecimal.ZERO) > 0) {
      log.info("User {} has exhausted all tiers, no higher tier available", userId);
      return CreditEligibilityResponse.builder()
          .canApply(false)
          .reason("Tidak ada tier plafond yang lebih tinggi tersedia")
          .reasonCode(CreditEligibilityResponse.REASON_NO_HIGHER_TIER)
          .eligiblePlafonds(Collections.emptyList())
          .build();
    }

    // Find minimum next tier for display
    PlafondResponse minimumNextTier = eligiblePlafonds.isEmpty() ? null : eligiblePlafonds.get(0);

    log.info(
        "User {} is eligible to apply. Minimum tier: {}",
        userId,
        minimumNextTier != null ? minimumNextTier.getName() : "Any");

    return CreditEligibilityResponse.builder()
        .canApply(true)
        .reason(
            minimumAmount.compareTo(BigDecimal.ZERO) > 0
                ? "Anda dapat mengajukan plafond dengan tier lebih tinggi"
                : "Anda dapat mengajukan plafond baru")
        .reasonCode(CreditEligibilityResponse.REASON_ELIGIBLE)
        .minimumNextTier(minimumNextTier)
        .eligiblePlafonds(eligiblePlafonds)
        .build();
  }

  /**
   * Get the minimum plafond amount required for next application. Returns 0 if user has never had a
   * plafond.
   */
  public BigDecimal getMinimumNextTierAmount(Long userId) {
    List<UserPlafond> highestApproved = userPlafondRepository.findHighestApprovedPlafond(userId);
    if (highestApproved.isEmpty()) {
      return BigDecimal.ZERO; // New user, can apply any tier
    }
    // Return the maxAmount of their highest approved plafond
    return highestApproved.get(0).getPlafond().getMaxAmount();
  }

  /** Get list of plafonds that are higher than the specified amount. */
  public List<PlafondResponse> getEligiblePlafonds(BigDecimal minimumAmount) {
    List<Plafond> allActive = plafondRepository.findAllActive();

    return allActive.stream()
        .filter(p -> p.getMaxAmount().compareTo(minimumAmount) > 0)
        .sorted((a, b) -> a.getMaxAmount().compareTo(b.getMaxAmount()))
        .map(this::mapToPlafondResponse)
        .collect(Collectors.toList());
  }

  /** Validate if user can apply for a specific plafond (tier-up check). */
  public void validatePlafondApplication(Long userId, Long plafondId) {
    CreditEligibilityResponse eligibility = checkEligibility(userId);

    if (!eligibility.isCanApply()) {
      throw new IllegalStateException(eligibility.getReason());
    }

    // If user has previous plafond, check tier-up requirement
    BigDecimal minimumAmount = getMinimumNextTierAmount(userId);
    if (minimumAmount.compareTo(BigDecimal.ZERO) > 0) {
      Plafond targetPlafond =
          plafondRepository
              .findById(plafondId)
              .orElseThrow(() -> new IllegalArgumentException("Plafond tidak ditemukan"));

      if (targetPlafond.getMaxAmount().compareTo(minimumAmount) <= 0) {
        throw new IllegalStateException(
            "Anda harus mengajukan plafond dengan tier lebih tinggi dari "
                + formatCurrency(minimumAmount));
      }
    }
  }

  // ========== HELPER METHODS ==========

  private ActiveLimitInfo mapToActiveLimitInfo(UserPlafond up) {
    return ActiveLimitInfo.builder()
        .applicationId(up.getId())
        .plafondName(up.getPlafond().getName())
        .plafondId(up.getPlafond().getId())
        .maxAmount(up.getPlafond().getMaxAmount())
        .approvedLimit(up.getApprovedLimit())
        .usedAmount(up.getUsedAmount())
        .remainingLimit(up.getAvailableLimit())
        .build();
  }

  private PlafondResponse mapToPlafondResponse(Plafond p) {
    return PlafondResponse.builder()
        .id(p.getId())
        .name(p.getName())
        .description(p.getDescription())
        .maxAmount(p.getMaxAmount())
        .isActive(p.getIsActive())
        .createdAt(p.getCreatedAt())
        .build();
  }

  private String formatCurrency(BigDecimal amount) {
    return String.format("%,.0f", amount);
  }
}
