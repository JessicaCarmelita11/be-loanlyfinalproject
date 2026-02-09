package com.example.loanlyFinalProject.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPlafondResponse {

  private Long id;
  private Long userId;
  private String username;
  private String customerFullName; // Added for frontend
  private String status;
  private LocalDateTime registeredAt;

  // Plafond info
  private PlafondInfo plafond;

  // Credit limit info
  private BigDecimal approvedLimit;
  private BigDecimal usedAmount;
  private BigDecimal availableLimit;

  // Applicant detail
  private ApplicantDetail applicantDetail;

  // Approval info
  private String reviewedByUsername;
  private LocalDateTime reviewedAt;
  private String reviewNote; // Marketing's note from history
  private String approvedByUsername;
  private LocalDateTime approvedAt;
  private String rejectionNote;

  // Documents
  private List<DocumentInfo> documents;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PlafondInfo {
    private Long id;
    private String name;
    private BigDecimal maxAmount;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ApplicantDetail {
    private String fullName; // Added for frontend
    private String nik;
    private String birthPlace;
    private LocalDate birthDate;
    private String maritalStatus;
    private String occupation;
    private BigDecimal monthlyIncome;
    private String phone;
    private String npwp;
    private String bankName; // Added for frontend
    private String accountNumber; // No Rekening

    // Location Data
    private BigDecimal applicationLatitude;
    private BigDecimal applicationLongitude;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class DocumentInfo {
    private Long id;
    private String documentType;
    private String fileUrl;
    private String fileName;
    private LocalDateTime uploadedAt;
  }
}
