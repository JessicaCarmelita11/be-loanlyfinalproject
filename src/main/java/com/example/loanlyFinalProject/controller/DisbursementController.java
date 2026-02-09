package com.example.loanlyFinalProject.controller;

import com.example.loanlyFinalProject.dto.request.DisbursementRequest;
import com.example.loanlyFinalProject.dto.response.ApiResponse;
import com.example.loanlyFinalProject.dto.response.DisbursementResponse;
import com.example.loanlyFinalProject.service.DisbursementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Disbursement", description = "Fund disbursement from approved credit limit")
@SecurityRequirement(name = "Bearer Authentication")
public class DisbursementController {

  private final DisbursementService disbursementService;

  // ========== CUSTOMER ENDPOINTS ==========

  @PostMapping("/customer/disbursements")
  @Operation(
      summary = "Request disbursement (Customer)",
      description = "Customer requests fund disbursement from approved credit limit")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<DisbursementResponse>> requestDisbursement(
      @RequestAttribute("userId") Long userId, @Valid @RequestBody DisbursementRequest request) {
    DisbursementResponse response = disbursementService.requestDisbursement(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                "Disbursement request submitted. Waiting for Back Office processing.", response));
  }

  @GetMapping("/customer/disbursements")
  @Operation(
      summary = "Get my disbursements (Customer)",
      description = "Returns all disbursement history")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<DisbursementResponse>>> getMyDisbursements(
      @RequestAttribute("userId") Long userId) {
    List<DisbursementResponse> disbursements = disbursementService.getMyDisbursements(userId);
    return ResponseEntity.ok(ApiResponse.success("Disbursements retrieved", disbursements));
  }

  // ========== SHARED HISTORY ENDPOINT (All Staff) ==========

  @GetMapping("/disbursements")
  @Operation(
      summary = "Get all disbursements (Staff)",
      description = "Returns all disbursement history for staff members")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MARKETING', 'BRANCH_MANAGER', 'BACK_OFFICE')")
  public ResponseEntity<ApiResponse<List<DisbursementResponse>>> getAllDisbursementsForStaff() {
    List<DisbursementResponse> disbursements = disbursementService.getAllDisbursements();
    return ResponseEntity.ok(ApiResponse.success("All disbursements retrieved", disbursements));
  }

  // ========== BACK OFFICE ENDPOINTS ==========

  @GetMapping("/back-office/disbursements/pending")
  @Operation(
      summary = "Get pending disbursements (Back Office)",
      description = "Returns disbursements waiting for processing")
  @PreAuthorize("hasAnyRole('BACK_OFFICE', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<DisbursementResponse>>> getPendingDisbursements() {
    List<DisbursementResponse> disbursements = disbursementService.getPendingDisbursements();
    return ResponseEntity.ok(ApiResponse.success("Pending disbursements retrieved", disbursements));
  }

  @PostMapping("/back-office/disbursements/{disbursementId}/process")
  @Operation(
      summary = "Process disbursement (Back Office)",
      description = "Back Office processes and disburses funds")
  @PreAuthorize("hasAnyRole('BACK_OFFICE', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<DisbursementResponse>> processDisbursement(
      @RequestAttribute("userId") Long userId,
      @PathVariable Long disbursementId,
      @RequestParam(required = false) String note) {
    DisbursementResponse response =
        disbursementService.processDisbursement(userId, disbursementId, note);
    return ResponseEntity.ok(
        ApiResponse.success("Disbursement has been processed successfully", response));
  }

  @PostMapping("/back-office/disbursements/{disbursementId}/cancel")
  @Operation(
      summary = "Cancel disbursement (Back Office)",
      description = "Back Office cancels disbursement request")
  @PreAuthorize("hasAnyRole('BACK_OFFICE', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<DisbursementResponse>> cancelDisbursement(
      @RequestAttribute("userId") Long userId,
      @PathVariable Long disbursementId,
      @RequestParam String reason) {
    DisbursementResponse response =
        disbursementService.cancelDisbursement(userId, disbursementId, reason);
    return ResponseEntity.ok(ApiResponse.success("Disbursement has been cancelled", response));
  }
}
