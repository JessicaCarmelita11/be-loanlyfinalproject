package com.example.loanlyFinalProject.controller;

import com.example.loanlyFinalProject.dto.request.PlafondApplicationRequest;
import com.example.loanlyFinalProject.dto.request.PlafondReviewRequest;
import com.example.loanlyFinalProject.dto.response.ApiResponse;
import com.example.loanlyFinalProject.dto.response.UserPlafondResponse;
import com.example.loanlyFinalProject.entity.PlafondDocument;
import com.example.loanlyFinalProject.service.PlafondApplicationService;
import com.example.loanlyFinalProject.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Plafond Application", description = "Credit limit application workflow")
@SecurityRequirement(name = "Bearer Authentication")
public class PlafondApplicationController {

  private final PlafondApplicationService applicationService;
  private final StorageService storageService;

  // ========== CUSTOMER ENDPOINTS ==========

  @PostMapping("/customer/plafonds/apply")
  @Operation(
      summary = "Apply for plafond (Customer)",
      description = "Customer applies for credit limit with documents")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<UserPlafondResponse>> applyForPlafond(
      @RequestAttribute("userId") Long userId,
      @Valid @RequestBody PlafondApplicationRequest request) {
    UserPlafondResponse response = applicationService.applyForPlafond(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Application submitted successfully", response));
  }

  @GetMapping("/customer/plafonds/applications")
  @Operation(
      summary = "Get my applications (Customer)",
      description = "Returns all plafond applications")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<UserPlafondResponse>>> getMyApplications(
      @RequestAttribute("userId") Long userId) {
    List<UserPlafondResponse> applications = applicationService.getMyApplications(userId);
    return ResponseEntity.ok(ApiResponse.success("Applications retrieved", applications));
  }

  @GetMapping("/customer/plafonds/approved")
  @Operation(
      summary = "Get approved credit lines (Customer)",
      description = "Returns approved plafonds with available limit")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<UserPlafondResponse>>> getApprovedPlafonds(
      @RequestAttribute("userId") Long userId) {
    List<UserPlafondResponse> approved = applicationService.getMyApprovedPlafonds(userId);
    return ResponseEntity.ok(ApiResponse.success("Approved plafonds retrieved", approved));
  }

  @GetMapping("/customer/plafonds/applications/{applicationId}")
  @Operation(
      summary = "Get application details (Customer)",
      description = "Returns specific application details")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<UserPlafondResponse>> getApplicationDetails(
      @PathVariable Long applicationId) {
    UserPlafondResponse application = applicationService.getApplicationById(applicationId);
    return ResponseEntity.ok(ApiResponse.success("Application retrieved", application));
  }

  @PostMapping(
      value = "/customer/plafonds/applications/{applicationId}/documents",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Upload document (Customer)",
      description = "Upload document for plafond application")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDocument(
      @PathVariable Long applicationId,
      @RequestParam("file") MultipartFile file,
      @RequestParam("documentType") PlafondDocument.DocumentType documentType) {
    PlafondDocument document =
        storageService.uploadPlafondDocument(applicationId, file, documentType);
    return ResponseEntity.ok(
        ApiResponse.success(
            "Document uploaded successfully",
            Map.of(
                "documentId", document.getId(),
                "fileUrl", document.getFileUrl(),
                "documentType", document.getDocumentType().name())));
  }

  // ========== MARKETING ENDPOINTS ==========

  @GetMapping("/marketing/plafond-applications/pending")
  @Operation(
      summary = "Get pending applications (Marketing)",
      description = "Returns applications waiting for review")
  @PreAuthorize("hasAnyRole('MARKETING', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<UserPlafondResponse>>> getPendingReviewApplications() {
    List<UserPlafondResponse> applications = applicationService.getPendingReviewApplications();
    return ResponseEntity.ok(ApiResponse.success("Pending applications retrieved", applications));
  }

  @PostMapping("/marketing/plafond-applications/review")
  @Operation(
      summary = "Review application (Marketing)",
      description = "Marketing reviews and forwards to Branch Manager")
  @PreAuthorize("hasAnyRole('MARKETING', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<UserPlafondResponse>> reviewApplication(
      @RequestAttribute("userId") Long userId, @Valid @RequestBody PlafondReviewRequest request) {
    UserPlafondResponse response = applicationService.reviewApplication(userId, request);
    String message =
        response.getStatus().equals("WAITING_APPROVAL")
            ? "Application approved and forwarded to Branch Manager"
            : "Application has been rejected";
    return ResponseEntity.ok(ApiResponse.success(message, response));
  }

  // ========== BRANCH MANAGER ENDPOINTS ==========

  @GetMapping("/branch-manager/plafond-applications/pending")
  @Operation(
      summary = "Get waiting approval (Branch Manager)",
      description = "Returns applications waiting for final approval")
  @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<UserPlafondResponse>>> getWaitingApprovalApplications() {
    List<UserPlafondResponse> applications = applicationService.getWaitingApprovalApplications();
    return ResponseEntity.ok(
        ApiResponse.success("Waiting approval applications retrieved", applications));
  }

  @PostMapping("/branch-manager/plafond-applications/approve")
  @Operation(
      summary = "Approve application (Branch Manager)",
      description = "Branch Manager approves with credit limit")
  @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<UserPlafondResponse>> approveApplication(
      @RequestAttribute("userId") Long userId, @Valid @RequestBody PlafondReviewRequest request) {
    UserPlafondResponse response = applicationService.approveApplication(userId, request);
    String message =
        response.getStatus().equals("APPROVED")
            ? "Application approved with credit limit: " + response.getApprovedLimit()
            : "Application has been rejected";
    return ResponseEntity.ok(ApiResponse.success(message, response));
  }

  // ========== SHARED HISTORY ENDPOINTS (All Staff) ==========

  @GetMapping("/plafond-histories")
  @Operation(
      summary = "Get all plafond histories (Staff)",
      description = "Returns all plafond status change histories for staff members")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MARKETING', 'BRANCH_MANAGER', 'BACK_OFFICE')")
  public ResponseEntity<ApiResponse<List<PlafondHistoryResponse>>> getPlafondHistoriesForStaff() {
    List<PlafondHistoryResponse> histories = applicationService.getAllPlafondHistories();
    return ResponseEntity.ok(ApiResponse.success("All plafond histories retrieved", histories));
  }

  // ========== ADMIN ENDPOINTS ==========

  @GetMapping("/admin/plafond-applications/{applicationId}")
  @Operation(
      summary = "Get any application (Admin)",
      description = "Admin can view any application details")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<UserPlafondResponse>> getAnyApplication(
      @PathVariable Long applicationId) {
    UserPlafondResponse application = applicationService.getApplicationById(applicationId);
    return ResponseEntity.ok(ApiResponse.success("Application retrieved", application));
  }

  @GetMapping("/admin/customers/approved")
  @Operation(
      summary = "Get approved customers (Admin)",
      description = "Returns customers with approved plafonds")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MARKETING', 'BRANCH_MANAGER', 'BACK_OFFICE')")
  public ResponseEntity<ApiResponse<List<ApprovedCustomerResponse>>> getApprovedCustomers() {
    List<ApprovedCustomerResponse> customers = applicationService.getApprovedCustomers();
    return ResponseEntity.ok(ApiResponse.success("Approved customers retrieved", customers));
  }

  @lombok.Getter
  @lombok.Setter
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  @lombok.Builder
  public static class ApprovedCustomerResponse {
    private Long applicationId;
    private Long customerId;
    private String customerUsername;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String plafondName;
    private java.math.BigDecimal approvedLimit;
    private java.math.BigDecimal usedAmount;
    private java.math.BigDecimal availableLimit;
    private LocalDateTime approvedAt;
  }

  @GetMapping("/admin/plafond-applications/{applicationId}/history")
  @Operation(
      summary = "Get application history (Admin)",
      description = "Returns status change history for an application")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MARKETING', 'BRANCH_MANAGER')")
  public ResponseEntity<ApiResponse<List<PlafondHistoryResponse>>> getApplicationHistory(
      @PathVariable Long applicationId) {
    List<PlafondHistoryResponse> history = applicationService.getApplicationHistory(applicationId);
    return ResponseEntity.ok(ApiResponse.success("Application history retrieved", history));
  }

  @GetMapping("/admin/plafond-histories")
  @Operation(
      summary = "Get all plafond histories (Admin)",
      description = "Returns all plafond status change histories")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MARKETING', 'BRANCH_MANAGER')")
  public ResponseEntity<ApiResponse<List<PlafondHistoryResponse>>> getAllPlafondHistories() {
    List<PlafondHistoryResponse> histories = applicationService.getAllPlafondHistories();
    return ResponseEntity.ok(ApiResponse.success("All plafond histories retrieved", histories));
  }

  @lombok.Getter
  @lombok.Setter
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  @lombok.Builder
  public static class PlafondHistoryResponse {
    private Long id;
    private Long applicationId;
    private String customerUsername;
    private String customerName; // Full name for frontend
    private String plafondName;
    private String previousStatus;
    private String newStatus;
    private String actionByUsername;
    private String actionByRole;
    private String note;
    private LocalDateTime createdAt;
  }
}
