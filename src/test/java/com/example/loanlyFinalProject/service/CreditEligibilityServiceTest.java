package com.example.loanlyFinalProject.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.example.loanlyFinalProject.dto.response.CreditEligibilityResponse;
import com.example.loanlyFinalProject.entity.Plafond;
import com.example.loanlyFinalProject.entity.UserPlafond;
import com.example.loanlyFinalProject.repository.PlafondRepository;
import com.example.loanlyFinalProject.repository.UserPlafondRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditEligibilityService Unit Tests")
class CreditEligibilityServiceTest {

  @Mock private UserPlafondRepository userPlafondRepository;

  @Mock private PlafondRepository plafondRepository;

  @InjectMocks private CreditEligibilityService creditEligibilityService;

  private Plafond plusPlafond;
  private Plafond bronzePlafond;
  private Plafond silverPlafond;
  private Plafond goldPlafond;

  @BeforeEach
  void setUp() {
    // Setup test plafonds with different tiers
    plusPlafond =
        Plafond.builder()
            .id(1L)
            .name("Plus")
            .maxAmount(new BigDecimal("5000000"))
            .isActive(true)
            .build();

    bronzePlafond =
        Plafond.builder()
            .id(2L)
            .name("Bronze")
            .maxAmount(new BigDecimal("15000000"))
            .isActive(true)
            .build();

    silverPlafond =
        Plafond.builder()
            .id(3L)
            .name("Silver")
            .maxAmount(new BigDecimal("25000000"))
            .isActive(true)
            .build();

    goldPlafond =
        Plafond.builder()
            .id(4L)
            .name("Gold")
            .maxAmount(new BigDecimal("50000000"))
            .isActive(true)
            .build();
  }

  @Test
  @DisplayName("New user (no previous plafond) - Should be eligible for any plafond")
  void checkEligibility_NewUser_ShouldBeEligibleForAnyPlafond() {
    // Arrange
    Long userId = 1L;
    when(userPlafondRepository.hasPendingApplication(userId)).thenReturn(false);
    when(userPlafondRepository.findActiveWithRemainingLimit(userId))
        .thenReturn(Collections.emptyList());
    when(userPlafondRepository.findHighestApprovedPlafond(userId))
        .thenReturn(Collections.emptyList());
    when(plafondRepository.findAllActive())
        .thenReturn(List.of(plusPlafond, bronzePlafond, silverPlafond, goldPlafond));

    // Act
    CreditEligibilityResponse response = creditEligibilityService.checkEligibility(userId);

    // Assert
    assertTrue(response.isCanApply());
    assertEquals(CreditEligibilityResponse.REASON_ELIGIBLE, response.getReasonCode());
    assertEquals(4, response.getEligiblePlafonds().size()); // All plafonds available
    assertNull(response.getCurrentLimit());

    verify(userPlafondRepository).hasPendingApplication(userId);
    verify(userPlafondRepository).findActiveWithRemainingLimit(userId);
  }

  @Test
  @DisplayName("User with active limit (remaining > 0) - Should NOT be eligible")
  void checkEligibility_ActiveLimitExists_ShouldNotBeEligible() {
    // Arrange
    Long userId = 2L;

    UserPlafond activeUserPlafond =
        UserPlafond.builder()
            .id(10L)
            .plafond(silverPlafond)
            .approvedLimit(new BigDecimal("20000000"))
            .usedAmount(new BigDecimal("15000000")) // Remaining = 5,000,000
            .status(UserPlafond.PlafondApplicationStatus.APPROVED)
            .build();

    when(userPlafondRepository.hasPendingApplication(userId)).thenReturn(false);
    when(userPlafondRepository.findActiveWithRemainingLimit(userId))
        .thenReturn(List.of(activeUserPlafond));

    // Act
    CreditEligibilityResponse response = creditEligibilityService.checkEligibility(userId);

    // Assert
    assertFalse(response.isCanApply());
    assertEquals(CreditEligibilityResponse.REASON_ACTIVE_LIMIT_EXISTS, response.getReasonCode());
    assertNotNull(response.getCurrentLimit());
    assertEquals("Silver", response.getCurrentLimit().getPlafondName());
    assertEquals(new BigDecimal("5000000"), response.getCurrentLimit().getRemainingLimit());
    assertTrue(response.getEligiblePlafonds().isEmpty());

    // Should not check for tier-up since blocked by active limit
    verify(userPlafondRepository, never()).findHighestApprovedPlafond(anyLong());
  }

  @Test
  @DisplayName("User with pending application - Should NOT be eligible")
  void checkEligibility_PendingApplication_ShouldNotBeEligible() {
    // Arrange
    Long userId = 3L;
    when(userPlafondRepository.hasPendingApplication(userId)).thenReturn(true);

    // Act
    CreditEligibilityResponse response = creditEligibilityService.checkEligibility(userId);

    // Assert
    assertFalse(response.isCanApply());
    assertEquals(CreditEligibilityResponse.REASON_PENDING_APPLICATION, response.getReasonCode());
    assertTrue(response.getEligiblePlafonds().isEmpty());

    // Should not check other conditions since blocked by pending application
    verify(userPlafondRepository, never()).findActiveWithRemainingLimit(anyLong());
    verify(userPlafondRepository, never()).findHighestApprovedPlafond(anyLong());
  }

  @Test
  @DisplayName("User with exhausted limit - Should be eligible for higher tier only")
  void checkEligibility_ExhaustedLimit_ShouldBeEligibleForHigherTierOnly() {
    // Arrange
    Long userId = 4L;

    // User had Bronze plafond (maxAmount = 15,000,000) that is now exhausted
    UserPlafond exhaustedUserPlafond =
        UserPlafond.builder()
            .id(20L)
            .plafond(bronzePlafond)
            .approvedLimit(new BigDecimal("15000000"))
            .usedAmount(new BigDecimal("15000000")) // Fully used, remaining = 0
            .status(UserPlafond.PlafondApplicationStatus.APPROVED)
            .build();

    when(userPlafondRepository.hasPendingApplication(userId)).thenReturn(false);
    when(userPlafondRepository.findActiveWithRemainingLimit(userId))
        .thenReturn(Collections.emptyList()); // No
    // active
    // limit
    when(userPlafondRepository.findHighestApprovedPlafond(userId))
        .thenReturn(List.of(exhaustedUserPlafond));
    when(plafondRepository.findAllActive())
        .thenReturn(List.of(plusPlafond, bronzePlafond, silverPlafond, goldPlafond));

    // Act
    CreditEligibilityResponse response = creditEligibilityService.checkEligibility(userId);

    // Assert
    assertTrue(response.isCanApply());
    assertEquals(CreditEligibilityResponse.REASON_ELIGIBLE, response.getReasonCode());

    // Only plafonds with maxAmount > 15,000,000 should be eligible (Silver, Gold)
    assertEquals(2, response.getEligiblePlafonds().size());

    // First eligible should be Silver (next tier up from Bronze)
    assertEquals("Silver", response.getEligiblePlafonds().get(0).getName());
    assertEquals(new BigDecimal("25000000"), response.getEligiblePlafonds().get(0).getMaxAmount());

    // Second should be Gold
    assertEquals("Gold", response.getEligiblePlafonds().get(1).getName());

    // Plus and Bronze should NOT be in eligible list (lower or equal tier)
    assertTrue(
        response.getEligiblePlafonds().stream()
            .noneMatch(p -> p.getName().equals("Plus") || p.getName().equals("Bronze")));

    // Minimum next tier should be Silver
    assertNotNull(response.getMinimumNextTier());
    assertEquals("Silver", response.getMinimumNextTier().getName());
  }
}
