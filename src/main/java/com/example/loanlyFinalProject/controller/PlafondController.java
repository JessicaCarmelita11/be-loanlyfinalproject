package com.example.loanlyFinalProject.controller;

import com.example.loanlyFinalProject.dto.request.PlafondRequest;
import com.example.loanlyFinalProject.dto.response.ApiResponse;
import com.example.loanlyFinalProject.dto.response.PlafondResponse;
import com.example.loanlyFinalProject.service.PlafondService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Plafond", description = "Plafond management endpoints")
public class PlafondController {

  private final PlafondService plafondService;
  private final com.example.loanlyFinalProject.service.TenorRateService tenorRateService;

  // ========== PUBLIC ENDPOINTS (No Auth Required) ==========

  @GetMapping("/api/public/plafonds")
  @Operation(
      summary = "Get all active plafonds (Public)",
      description = "Returns list of all active plafonds without authentication")
  public ResponseEntity<ApiResponse<List<PlafondResponse>>> getAllActivePlafonds() {
    List<PlafondResponse> plafonds = plafondService.getAllActivePlafonds();
    return ResponseEntity.ok(ApiResponse.success("Plafonds retrieved successfully", plafonds));
  }

  @GetMapping("/api/public/plafonds/{id}")
  @Operation(
      summary = "Get plafond by ID (Public)",
      description = "Returns plafond details by ID without authentication")
  public ResponseEntity<ApiResponse<PlafondResponse>> getPlafondById(@PathVariable Long id) {
    PlafondResponse plafond = plafondService.getPlafondById(id);
    return ResponseEntity.ok(ApiResponse.success("Plafond retrieved successfully", plafond));
  }

  @GetMapping("/api/public/plafonds/{id}/rates")
  @Operation(
      summary = "Get tenor rates by plafond ID (Public)",
      description = "Returns tenor rates for a specific plafond without authentication")
  public ResponseEntity<
          ApiResponse<List<com.example.loanlyFinalProject.dto.response.TenorRateResponse>>>
      getRatesByPlafondIdPublic(@PathVariable Long id) {
    List<com.example.loanlyFinalProject.dto.response.TenorRateResponse> rates =
        tenorRateService.getRatesByPlafondId(id);
    return ResponseEntity.ok(ApiResponse.success("Tenor rates retrieved successfully", rates));
  }

  // ========== ADMIN ENDPOINTS (Requires SUPER_ADMIN role) ==========

  @GetMapping("/api/admin/plafonds")
  @Operation(
      summary = "Get all plafonds with pagination (Admin)",
      description = "Returns paginated list of all plafonds")
  @SecurityRequirement(name = "Bearer Authentication")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<Page<PlafondResponse>>> getAllPlafonds(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDir,
      @RequestParam(required = false) String search) {

    Sort sort =
        sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<PlafondResponse> plafonds;
    if (search != null && !search.isEmpty()) {
      plafonds = plafondService.searchPlafonds(search, pageable);
    } else {
      plafonds = plafondService.getAllPlafonds(pageable);
    }

    return ResponseEntity.ok(ApiResponse.success("Plafonds retrieved successfully", plafonds));
  }

  @GetMapping("/api/admin/plafonds/{id}")
  @Operation(
      summary = "Get plafond by ID (Admin)",
      description = "Returns plafond details by ID for admin")
  @SecurityRequirement(name = "Bearer Authentication")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<PlafondResponse>> getPlafondByIdAdmin(@PathVariable Long id) {
    PlafondResponse plafond = plafondService.getPlafondById(id);
    return ResponseEntity.ok(ApiResponse.success("Plafond retrieved successfully", plafond));
  }

  @PostMapping("/api/admin/plafonds")
  @Operation(summary = "Create new plafond (Admin)", description = "Creates a new plafond")
  @SecurityRequirement(name = "Bearer Authentication")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<PlafondResponse>> createPlafond(
      @Valid @RequestBody PlafondRequest request) {
    PlafondResponse plafond = plafondService.createPlafond(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Plafond created successfully", plafond));
  }

  @PutMapping("/api/admin/plafonds/{id}")
  @Operation(summary = "Update plafond (Admin)", description = "Updates an existing plafond")
  @SecurityRequirement(name = "Bearer Authentication")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<PlafondResponse>> updatePlafond(
      @PathVariable Long id, @Valid @RequestBody PlafondRequest request) {
    PlafondResponse plafond = plafondService.updatePlafond(id, request);
    return ResponseEntity.ok(ApiResponse.success("Plafond updated successfully", plafond));
  }

  @DeleteMapping("/api/admin/plafonds/{id}")
  @Operation(summary = "Delete plafond (Admin)", description = "Soft deletes a plafond")
  @SecurityRequirement(name = "Bearer Authentication")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<Object>> deletePlafond(@PathVariable Long id) {
    plafondService.deletePlafond(id);
    return ResponseEntity.ok(ApiResponse.success("Plafond deleted successfully"));
  }

  // ========== CUSTOMER ENDPOINTS ==========

  @PostMapping("/api/customer/plafonds/{plafondId}/register")
  @Operation(
      summary = "Register to plafond (Customer)",
      description = "Customer registers interest in a plafond")
  @SecurityRequirement(name = "Bearer Authentication")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<Object>> registerToPlafond(
      @PathVariable Long plafondId, @RequestAttribute("userId") Long userId) {
    plafondService.registerUserToPlafond(userId, plafondId);
    return ResponseEntity.ok(ApiResponse.success("Successfully registered to plafond"));
  }

  @GetMapping("/api/customer/my-plafonds")
  @Operation(
      summary = "Get my registered plafonds (Customer)",
      description = "Returns list of plafonds customer has registered to")
  @SecurityRequirement(name = "Bearer Authentication")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<PlafondResponse>>> getMyPlafonds(
      @RequestAttribute("userId") Long userId) {
    List<PlafondResponse> plafonds = plafondService.getUserPlafonds(userId);
    return ResponseEntity.ok(ApiResponse.success("My plafonds retrieved successfully", plafonds));
  }
}
