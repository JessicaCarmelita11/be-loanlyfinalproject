package com.example.loanlyFinalProject.controller;

import com.example.loanlyFinalProject.dto.response.ApiResponse;
import com.example.loanlyFinalProject.dto.response.CreditEligibilityResponse;
import com.example.loanlyFinalProject.service.CreditEligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@Tag(
    name = "Credit Eligibility",
    description = "Endpoints for checking credit eligibility and tier-up requirements")
public class CreditEligibilityController {

  private final CreditEligibilityService creditEligibilityService;

  @GetMapping("/credit-eligibility")
  @Operation(
      summary = "Check credit eligibility",
      description =
          "Check if customer can apply for a new plafond. "
              + "Returns eligibility status, current active limit info, and eligible plafonds for tier-up.")
  @SecurityRequirement(name = "Bearer Authentication")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CreditEligibilityResponse>> checkEligibility(
      @RequestAttribute("userId") Long userId) {

    CreditEligibilityResponse response = creditEligibilityService.checkEligibility(userId);

    String message =
        response.isCanApply()
            ? "Anda dapat mengajukan plafond baru"
            : "Anda belum dapat mengajukan plafond baru";

    return ResponseEntity.ok(ApiResponse.success(message, response));
  }

  @GetMapping("/eligible-plafonds")
  @Operation(
      summary = "Get eligible plafonds for tier-up",
      description =
          "Get list of plafonds that are eligible for tier-up (higher than previous tier).")
  @SecurityRequirement(name = "Bearer Authentication")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CreditEligibilityResponse>> getEligiblePlafonds(
      @RequestAttribute("userId") Long userId) {

    CreditEligibilityResponse response = creditEligibilityService.checkEligibility(userId);
    return ResponseEntity.ok(ApiResponse.success("Eligible plafonds retrieved", response));
  }
}
