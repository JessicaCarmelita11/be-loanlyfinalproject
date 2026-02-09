package com.example.loanlyFinalProject.controller;

import com.example.loanlyFinalProject.dto.request.TenorRateRequest;
import com.example.loanlyFinalProject.dto.response.ApiResponse;
import com.example.loanlyFinalProject.dto.response.TenorRateResponse;
import com.example.loanlyFinalProject.service.TenorRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tenor-rates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Tenor Rates (Admin)", description = "Manage interest rates per plafond tier")
public class TenorRateController {

  private final TenorRateService tenorRateService;

  @GetMapping
  @Operation(summary = "Get all rates", description = "Get all active tenor rates")
  public ResponseEntity<ApiResponse<List<TenorRateResponse>>> getAllRates() {
    List<TenorRateResponse> rates = tenorRateService.getAllRates();
    return ResponseEntity.ok(ApiResponse.success("Tenor rates retrieved", rates));
  }

  @GetMapping("/grouped")
  @Operation(
      summary = "Get rates grouped by plafond",
      description = "Get all rates organized by plafond tier")
  public ResponseEntity<ApiResponse<Map<String, List<TenorRateResponse>>>>
      getRatesGroupedByPlafond() {
    Map<String, List<TenorRateResponse>> rates = tenorRateService.getAllRatesGroupedByPlafond();
    return ResponseEntity.ok(ApiResponse.success("Tenor rates retrieved", rates));
  }

  @GetMapping("/plafond/{plafondId}")
  @Operation(
      summary = "Get rates for plafond",
      description = "Get all tenor rates for a specific plafond tier")
  public ResponseEntity<ApiResponse<List<TenorRateResponse>>> getRatesByPlafond(
      @PathVariable Long plafondId) {
    List<TenorRateResponse> rates = tenorRateService.getRatesByPlafondId(plafondId);
    return ResponseEntity.ok(ApiResponse.success("Tenor rates for plafond retrieved", rates));
  }

  @PostMapping
  @Operation(
      summary = "Create new rate",
      description = "Create a new interest rate for a plafond-tenor combination")
  public ResponseEntity<ApiResponse<TenorRateResponse>> createRate(
      @Valid @RequestBody TenorRateRequest request) {
    TenorRateResponse rate = tenorRateService.createRate(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tenor rate created successfully", rate));
  }

  @PutMapping("/{rateId}")
  @Operation(summary = "Update rate", description = "Update an existing interest rate")
  public ResponseEntity<ApiResponse<TenorRateResponse>> updateRate(
      @PathVariable Long rateId, @Valid @RequestBody TenorRateRequest request) {
    TenorRateResponse rate = tenorRateService.updateRate(rateId, request);
    return ResponseEntity.ok(ApiResponse.success("Tenor rate updated successfully", rate));
  }

  @DeleteMapping("/{rateId}")
  @Operation(summary = "Delete rate", description = "Soft delete an interest rate")
  public ResponseEntity<ApiResponse<Object>> deleteRate(@PathVariable Long rateId) {
    tenorRateService.deleteRate(rateId);
    return ResponseEntity.ok(ApiResponse.success("Tenor rate deleted successfully"));
  }
}
