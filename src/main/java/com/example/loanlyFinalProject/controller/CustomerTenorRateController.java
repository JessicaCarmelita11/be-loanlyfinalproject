package com.example.loanlyFinalProject.controller;

import com.example.loanlyFinalProject.dto.response.ApiResponse;
import com.example.loanlyFinalProject.dto.response.TenorRateResponse;
import com.example.loanlyFinalProject.entity.UserPlafond;
import com.example.loanlyFinalProject.exception.ResourceNotFoundException;
import com.example.loanlyFinalProject.repository.UserPlafondRepository;
import com.example.loanlyFinalProject.service.TenorRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/tenor-rates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(
    name = "Tenor Rates (Customer)",
    description = "Get interest rates for customer's plafond tier")
public class CustomerTenorRateController {

  private final TenorRateService tenorRateService;
  private final UserPlafondRepository userPlafondRepository;

  @GetMapping("/my-plafond/{userPlafondId}")
  @Operation(
      summary = "Get rates for my plafond",
      description = "Get available tenor rates for customer's specific user plafond")
  public ResponseEntity<ApiResponse<List<TenorRateResponse>>> getRatesForMyPlafond(
      @PathVariable Long userPlafondId) {

    // Get user's plafond
    UserPlafond userPlafond =
        userPlafondRepository
            .findById(userPlafondId)
            .orElseThrow(() -> new ResourceNotFoundException("UserPlafond", "id", userPlafondId));

    // Get the plafond ID from user's active plafond
    Long plafondId = userPlafond.getPlafond().getId();

    // Get rates for that plafond tier
    List<TenorRateResponse> rates = tenorRateService.getRatesByPlafondId(plafondId);

    return ResponseEntity.ok(
        ApiResponse.success("Tenor rates for your plafond tier retrieved successfully", rates));
  }

  @GetMapping("/plafond/{plafondId}")
  @Operation(
      summary = "Get rates by plafond ID",
      description = "Get available tenor rates for a specific plafond tier")
  public ResponseEntity<ApiResponse<List<TenorRateResponse>>> getRatesByPlafondId(
      @PathVariable Long plafondId) {

    List<TenorRateResponse> rates = tenorRateService.getRatesByPlafondId(plafondId);
    return ResponseEntity.ok(ApiResponse.success("Tenor rates retrieved successfully", rates));
  }
}
