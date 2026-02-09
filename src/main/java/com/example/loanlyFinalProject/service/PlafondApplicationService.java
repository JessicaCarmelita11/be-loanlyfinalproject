package com.example.loanlyFinalProject.service;

import com.example.loanlyFinalProject.dto.request.PlafondApplicationRequest;
import com.example.loanlyFinalProject.dto.request.PlafondReviewRequest;
import com.example.loanlyFinalProject.dto.response.UserPlafondResponse;
import com.example.loanlyFinalProject.entity.*;
import com.example.loanlyFinalProject.exception.DuplicateResourceException;
import com.example.loanlyFinalProject.exception.ResourceNotFoundException;
import com.example.loanlyFinalProject.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlafondApplicationService {

  private final UserPlafondRepository userPlafondRepository;
  private final PlafondRepository plafondRepository;
  private final PlafondHistoryRepository plafondHistoryRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;
  private final CreditEligibilityService creditEligibilityService;

  // ========== CUSTOMER: Apply for Plafond ==========

  @Transactional
  public UserPlafondResponse applyForPlafond(Long userId, PlafondApplicationRequest request) {
    // Validate credit eligibility and tier-up requirement
    creditEligibilityService.validatePlafondApplication(userId, request.getPlafondId());

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

    Plafond plafond =
        plafondRepository
            .findByIdNotDeleted(request.getPlafondId())
            .orElseThrow(
                () -> new ResourceNotFoundException("Plafond", "id", request.getPlafondId()));

    // Check if user already has active application for this plafond
    if (userPlafondRepository.existsActiveByUserIdAndPlafondId(userId, request.getPlafondId())) {
      throw new DuplicateResourceException(
          "User already has an active application for this plafond");
    }

    // Create user plafond application
    UserPlafond userPlafond =
        UserPlafond.builder()
            .user(user)
            .plafond(plafond)
            .status(UserPlafond.PlafondApplicationStatus.PENDING_REVIEW)
            .nik(request.getNik())
            .birthPlace(request.getBirthPlace())
            .birthDate(request.getBirthDate())
            .maritalStatus(request.getMaritalStatus())
            .occupation(request.getOccupation())
            .monthlyIncome(request.getMonthlyIncome())
            .phone(request.getPhone())
            .npwp(request.getNpwp())
            .bankName(request.getBankName())
            .accountNumber(request.getAccountNumber())
            .applicationLatitude(request.getLatitude())
            .applicationLongitude(request.getLongitude())
            .build();

    UserPlafond saved = userPlafondRepository.save(userPlafond);

    // Create history
    createHistory(
        saved,
        null,
        UserPlafond.PlafondApplicationStatus.PENDING_REVIEW,
        user,
        "CUSTOMER",
        "Application submitted");

    // Send notification
    notificationService.createNotification(
        userId,
        "Pengajuan Limit Kredit Diterima",
        "Pengajuan limit kredit Anda untuk " + plafond.getName() + " sedang dalam proses review.",
        Notification.NotificationType.LOAN_SUBMITTED,
        saved.getId());

    log.info(
        "Plafond application submitted by user: {} for plafond: {}",
        user.getUsername(),
        plafond.getName());

    return mapToResponse(saved);
  }

  // ========== CUSTOMER: Get My Applications ==========

  public List<UserPlafondResponse> getMyApplications(Long userId) {
    return userPlafondRepository.findByUserIdWithPlafond(userId).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  public List<UserPlafondResponse> getMyApprovedPlafonds(Long userId) {
    return userPlafondRepository.findApprovedByUserId(userId).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  // ========== MARKETING: Review Applications ==========

  public List<UserPlafondResponse> getPendingReviewApplications() {
    return userPlafondRepository.findAllPendingReview().stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public UserPlafondResponse reviewApplication(Long marketingUserId, PlafondReviewRequest request) {
    User marketingUser =
        userRepository
            .findById(marketingUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", marketingUserId));

    UserPlafond application =
        userPlafondRepository
            .findByIdWithDetails(request.getApplicationId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Application", "id", request.getApplicationId()));

    if (application.getStatus() != UserPlafond.PlafondApplicationStatus.PENDING_REVIEW) {
      throw new IllegalStateException("Application is not in PENDING_REVIEW status");
    }

    UserPlafond.PlafondApplicationStatus previousStatus = application.getStatus();
    UserPlafond.PlafondApplicationStatus newStatus;

    if (request.getApproved()) {
      newStatus = UserPlafond.PlafondApplicationStatus.WAITING_APPROVAL;
      application.setReviewedBy(marketingUser);
      application.setReviewedAt(LocalDateTime.now());
    } else {
      newStatus = UserPlafond.PlafondApplicationStatus.REJECTED;
      application.setRejectionNote(request.getNote());
    }

    application.setStatus(newStatus);
    UserPlafond saved = userPlafondRepository.save(application);

    // Create history
    createHistory(saved, previousStatus, newStatus, marketingUser, "MARKETING", request.getNote());

    // Send notification
    if (request.getApproved()) {
      notificationService.createNotification(
          application.getUser().getId(),
          "Pengajuan Disetujui Marketing",
          "Pengajuan limit kredit Anda telah diverifikasi dan menunggu persetujuan final.",
          Notification.NotificationType.LOAN_REVIEWED,
          saved.getId());
    } else {
      notificationService.createNotification(
          application.getUser().getId(),
          "Pengajuan Ditolak",
          "Mohon maaf, pengajuan limit kredit Anda tidak memenuhi kriteria. " + request.getNote(),
          Notification.NotificationType.LOAN_REJECTED,
          saved.getId());
    }

    log.info(
        "Application {} reviewed by Marketing: {} - Result: {}",
        application.getId(),
        marketingUser.getUsername(),
        newStatus);

    return mapToResponse(saved);
  }

  // ========== BRANCH MANAGER: Approve Applications ==========

  public List<UserPlafondResponse> getWaitingApprovalApplications() {
    return userPlafondRepository.findAllWaitingApproval().stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public UserPlafondResponse approveApplication(
      Long branchManagerId, PlafondReviewRequest request) {
    User branchManager =
        userRepository
            .findById(branchManagerId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", branchManagerId));

    UserPlafond application =
        userPlafondRepository
            .findByIdWithDetails(request.getApplicationId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Application", "id", request.getApplicationId()));

    if (application.getStatus() != UserPlafond.PlafondApplicationStatus.WAITING_APPROVAL) {
      throw new IllegalStateException("Application is not in WAITING_APPROVAL status");
    }

    UserPlafond.PlafondApplicationStatus previousStatus = application.getStatus();
    UserPlafond.PlafondApplicationStatus newStatus;

    if (request.getApproved()) {
      // Validate approved limit - Branch Manager must set it manually
      BigDecimal approvedLimit = request.getApprovedLimit();
      if (approvedLimit == null || approvedLimit.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Approved limit is required for approval");
      }
      // Limit cannot exceed plafond's maxAmount
      if (approvedLimit.compareTo(application.getPlafond().getMaxAmount()) > 0) {
        throw new IllegalArgumentException(
            "Approved limit cannot exceed plafond max amount: "
                + application.getPlafond().getMaxAmount());
      }

      newStatus = UserPlafond.PlafondApplicationStatus.APPROVED;
      application.setApprovedBy(branchManager);
      application.setApprovedAt(LocalDateTime.now());
      application.setApprovedLimit(approvedLimit);
      application.setUsedAmount(BigDecimal.ZERO);
    } else {
      newStatus = UserPlafond.PlafondApplicationStatus.REJECTED;
      application.setRejectionNote(request.getNote());
    }

    application.setStatus(newStatus);
    UserPlafond saved = userPlafondRepository.save(application);

    // Create history
    createHistory(
        saved, previousStatus, newStatus, branchManager, "BRANCH_MANAGER", request.getNote());

    // Send notification
    if (request.getApproved()) {
      notificationService.createNotification(
          application.getUser().getId(),
          "Limit Kredit Disetujui!",
          "Selamat! Anda mendapat limit kredit sebesar Rp "
              + application.getApprovedLimit()
              + ". Anda dapat melakukan pencairan kapan saja.",
          Notification.NotificationType.LOAN_APPROVED,
          saved.getId());
    } else {
      notificationService.createNotification(
          application.getUser().getId(),
          "Pengajuan Ditolak",
          "Mohon maaf, pengajuan limit kredit Anda tidak disetujui. " + request.getNote(),
          Notification.NotificationType.LOAN_REJECTED,
          saved.getId());
    }

    log.info(
        "Application {} approved by Branch Manager: {} - Result: {}, Limit: {}",
        application.getId(),
        branchManager.getUsername(),
        newStatus,
        application.getApprovedLimit());

    return mapToResponse(saved);
  }

  // ========== Get Application Details ==========

  public UserPlafondResponse getApplicationById(Long applicationId) {
    UserPlafond application =
        userPlafondRepository
            .findByIdWithDetails(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));
    return mapToResponse(application);
  }

  // ========== HELPER METHODS ==========

  private void createHistory(
      UserPlafond application,
      UserPlafond.PlafondApplicationStatus previousStatus,
      UserPlafond.PlafondApplicationStatus newStatus,
      User actionBy,
      String role,
      String note) {
    PlafondHistory history =
        PlafondHistory.builder()
            .userPlafond(application)
            .previousStatus(previousStatus != null ? previousStatus : newStatus)
            .newStatus(newStatus)
            .actionByUser(actionBy)
            .actionByRole(role)
            .note(note)
            .build();

    plafondHistoryRepository.save(history);
  }

  private UserPlafondResponse mapToResponse(UserPlafond up) {
    UserPlafondResponse.UserPlafondResponseBuilder builder =
        UserPlafondResponse.builder()
            .id(up.getId())
            .userId(up.getUser().getId())
            .username(up.getUser().getUsername())
            .status(up.getStatus().name())
            .registeredAt(up.getRegisteredAt())
            .approvedLimit(up.getApprovedLimit())
            .usedAmount(up.getUsedAmount())
            .availableLimit(up.getAvailableLimit());

    // Plafond info
    if (up.getPlafond() != null) {
      builder.plafond(
          UserPlafondResponse.PlafondInfo.builder()
              .id(up.getPlafond().getId())
              .name(up.getPlafond().getName())
              .maxAmount(up.getPlafond().getMaxAmount())
              .build());
    }

    // Customer full name
    builder.customerFullName(up.getUser().getFullName());

    // Applicant detail
    builder.applicantDetail(
        UserPlafondResponse.ApplicantDetail.builder()
            .fullName(up.getUser().getFullName())
            .nik(up.getNik())
            .birthPlace(up.getBirthPlace())
            .birthDate(up.getBirthDate())
            .maritalStatus(up.getMaritalStatus())
            .occupation(up.getOccupation())
            .monthlyIncome(up.getMonthlyIncome())
            .phone(up.getPhone())
            .npwp(up.getNpwp())
            .bankName(up.getBankName())
            .accountNumber(up.getAccountNumber())
            .applicationLatitude(up.getApplicationLatitude())
            .applicationLongitude(up.getApplicationLongitude())
            .build());

    // Approval info
    if (up.getReviewedBy() != null) {
      builder.reviewedByUsername(up.getReviewedBy().getUsername());
    }
    builder.reviewedAt(up.getReviewedAt());

    // Get Marketing's note from history table
    plafondHistoryRepository
        .findMarketingReviewByApplicationId(up.getId())
        .ifPresent(history -> builder.reviewNote(history.getNote()));

    if (up.getApprovedBy() != null) {
      builder.approvedByUsername(up.getApprovedBy().getUsername());
    }
    builder.approvedAt(up.getApprovedAt());
    builder.rejectionNote(up.getRejectionNote());

    // Documents
    if (up.getDocuments() != null && !up.getDocuments().isEmpty()) {
      builder.documents(
          up.getDocuments().stream()
              .map(
                  doc ->
                      UserPlafondResponse.DocumentInfo.builder()
                          .id(doc.getId())
                          .documentType(doc.getDocumentType().name())
                          .fileUrl(doc.getFileUrl())
                          .fileName(doc.getFileName())
                          .uploadedAt(doc.getUploadedAt())
                          .build())
              .collect(Collectors.toList()));
    }

    return builder.build();
  }

  // ========== APPROVED CUSTOMERS ==========

  public List<
          com.example
              .loanlyFinalProject
              .controller
              .PlafondApplicationController
              .ApprovedCustomerResponse>
      getApprovedCustomers() {
    List<UserPlafond> approvedPlafonds = userPlafondRepository.findAllApproved();

    return approvedPlafonds.stream()
        .map(
            up ->
                com.example
                    .loanlyFinalProject
                    .controller
                    .PlafondApplicationController
                    .ApprovedCustomerResponse
                    .builder()
                    .applicationId(up.getId())
                    .customerId(up.getUser().getId())
                    .customerUsername(up.getUser().getUsername())
                    .customerName(up.getUser().getFullName())
                    .customerEmail(up.getUser().getEmail())
                    .customerPhone(up.getUser().getPhone())
                    .plafondName(up.getPlafond().getName())
                    .approvedLimit(up.getApprovedLimit())
                    .usedAmount(up.getUsedAmount())
                    .availableLimit(up.getAvailableLimit())
                    .approvedAt(up.getApprovedAt())
                    .build())
        .collect(Collectors.toList());
  }

  // ========== HISTORY ==========

  public List<
          com.example
              .loanlyFinalProject
              .controller
              .PlafondApplicationController
              .PlafondHistoryResponse>
      getApplicationHistory(Long applicationId) {
    // Verify application exists
    userPlafondRepository
        .findById(applicationId)
        .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

    List<PlafondHistory> histories =
        plafondHistoryRepository.findByUserPlafondIdOrderByCreatedAtDesc(applicationId);

    return histories.stream().map(h -> mapToHistoryResponse(h)).collect(Collectors.toList());
  }

  public List<
          com.example
              .loanlyFinalProject
              .controller
              .PlafondApplicationController
              .PlafondHistoryResponse>
      getAllPlafondHistories() {
    List<PlafondHistory> histories = plafondHistoryRepository.findAll();

    return histories.stream().map(h -> mapToHistoryResponse(h)).collect(Collectors.toList());
  }

  private com.example
          .loanlyFinalProject
          .controller
          .PlafondApplicationController
          .PlafondHistoryResponse
      mapToHistoryResponse(PlafondHistory h) {
    return com.example
        .loanlyFinalProject
        .controller
        .PlafondApplicationController
        .PlafondHistoryResponse
        .builder()
        .id(h.getId())
        .applicationId(h.getUserPlafond().getId())
        .customerUsername(h.getUserPlafond().getUser().getUsername())
        .customerName(h.getUserPlafond().getUser().getFullName())
        .plafondName(h.getUserPlafond().getPlafond().getName())
        .previousStatus(h.getPreviousStatus().name())
        .newStatus(h.getNewStatus().name())
        .actionByUsername(h.getActionByUser().getUsername())
        .actionByRole(h.getActionByRole())
        .note(h.getNote())
        .createdAt(h.getCreatedAt())
        .build();
  }
}
