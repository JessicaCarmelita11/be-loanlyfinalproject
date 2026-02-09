package com.example.loanlyFinalProject.service;

import com.example.loanlyFinalProject.dto.request.DisbursementRequest;
import com.example.loanlyFinalProject.dto.response.DisbursementResponse;
import com.example.loanlyFinalProject.entity.*;
import com.example.loanlyFinalProject.exception.ResourceNotFoundException;
import com.example.loanlyFinalProject.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class DisbursementService {

  private final DisbursementRepository disbursementRepository;
  private final UserPlafondRepository userPlafondRepository;
  private final UserRepository userRepository;
  private final TenorRateRepository tenorRateRepository;
  private final NotificationService notificationService;

  // ========== CUSTOMER: Request Disbursement ==========

  @Transactional
  public DisbursementResponse requestDisbursement(Long userId, DisbursementRequest request) {
    UserPlafond userPlafond =
        userPlafondRepository
            .findById(request.getUserPlafondId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "User Plafond", "id", request.getUserPlafondId()));

    // Validate ownership
    if (!userPlafond.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("You don't have access to this credit line");
    }

    // Validate status
    if (userPlafond.getStatus() != UserPlafond.PlafondApplicationStatus.APPROVED) {
      throw new IllegalStateException("Credit line is not approved yet");
    }

    // Validate available limit
    if (!userPlafond.canDisburse(request.getAmount())) {
      throw new IllegalArgumentException(
          "Insufficient credit limit. Available: " + userPlafond.getAvailableLimit());
    }

    // Validate tenor - must be one of: 1, 3, 6, 9, 12, 15, 18, 21, 24
    if (!request.isValidTenor()) {
      throw new IllegalArgumentException(
          "Invalid tenor. Please choose one of: " + DisbursementRequest.VALID_TENORS);
    }

    // Get interest rate from TenorRate table based on Plafond tier + tenor
    Integer selectedTenor = request.getTenorMonth();
    Long plafondId = userPlafond.getPlafond().getId();
    String plafondName = userPlafond.getPlafond().getName();

    TenorRate tenorRate =
        tenorRateRepository
            .findActiveByPlafondIdAndTenorMonth(plafondId, selectedTenor)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Tenor "
                            + selectedTenor
                            + " bulan tidak tersedia untuk tier "
                            + plafondName
                            + ". Silakan pilih tenor yang sesuai dengan tier Anda."));

    BigDecimal interestRate = tenorRate.getInterestRate();

    // Interest = amount × (rate/100) × tenor
    // WE ASSUME THE RATE IS MONTHLY INTEREST RATE (Bunga Bulanan)
    BigDecimal interestAmount =
        request
            .getAmount()
            .multiply(interestRate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP))
            .multiply(new BigDecimal(selectedTenor))
            .setScale(2, RoundingMode.HALF_UP);

    BigDecimal totalAmount = request.getAmount().add(interestAmount);

    log.info(
        "Tenor: {} months, Rate: {}%, Interest: {}, Total: {}",
        selectedTenor, interestRate, interestAmount, totalAmount);

    // Create disbursement
    Disbursement disbursement =
        Disbursement.builder()
            .userPlafond(userPlafond)
            .amount(request.getAmount())
            .interestRate(interestRate)
            .tenorMonth(selectedTenor) // Use customer-selected tenor
            .interestAmount(interestAmount)
            .totalAmount(totalAmount)
            .status(Disbursement.DisbursementStatus.PENDING)
            .requestLatitude(request.getLatitude())
            .requestLongitude(request.getLongitude())
            .build();

    Disbursement saved = disbursementRepository.save(disbursement);

    // Update used amount (reserve the limit)
    userPlafond.setUsedAmount(userPlafond.getUsedAmount().add(request.getAmount()));
    userPlafondRepository.save(userPlafond);

    // Send notification
    notificationService.createNotification(
        userId,
        "Permintaan Pencairan Dikirim",
        "Pencairan sebesar Rp "
            + request.getAmount()
            + " dengan tenor "
            + selectedTenor
            + " bulan sedang diproses. Total yang harus dibayar: Rp "
            + totalAmount,
        Notification.NotificationType.LOAN_SUBMITTED,
        saved.getId());

    log.info(
        "Disbursement requested by user: {} for amount: {} with tenor: {} months",
        userId,
        request.getAmount(),
        selectedTenor);

    return mapToResponse(saved, userPlafond);
  }

  // ========== CUSTOMER: Get My Disbursements ==========

  public List<DisbursementResponse> getMyDisbursements(Long userId) {
    return disbursementRepository.findByUserId(userId).stream()
        .map(d -> mapToResponse(d, d.getUserPlafond()))
        .collect(Collectors.toList());
  }

  // ========== BACK OFFICE: Get Pending Disbursements ==========

  public List<DisbursementResponse> getPendingDisbursements() {
    return disbursementRepository.findAllPending().stream()
        .map(d -> mapToResponse(d, d.getUserPlafond()))
        .collect(Collectors.toList());
  }

  // ========== ALL STAFF: Get All Disbursements ==========

  public List<DisbursementResponse> getAllDisbursements() {
    return disbursementRepository.findAll().stream()
        .map(d -> mapToResponse(d, d.getUserPlafond()))
        .collect(Collectors.toList());
  }

  // ========== BACK OFFICE: Process Disbursement ==========

  @Transactional
  public DisbursementResponse processDisbursement(
      Long backOfficeUserId, Long disbursementId, String note) {
    User backOfficeUser =
        userRepository
            .findById(backOfficeUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", backOfficeUserId));

    Disbursement disbursement =
        disbursementRepository
            .findById(disbursementId)
            .orElseThrow(() -> new ResourceNotFoundException("Disbursement", "id", disbursementId));

    if (disbursement.getStatus() != Disbursement.DisbursementStatus.PENDING) {
      throw new IllegalStateException("Disbursement is not in PENDING status");
    }

    disbursement.setStatus(Disbursement.DisbursementStatus.DISBURSED);
    disbursement.setDisbursedAt(LocalDateTime.now());
    disbursement.setDisbursedBy(backOfficeUser);
    disbursement.setNote(note);

    Disbursement saved = disbursementRepository.save(disbursement);

    // Send notification
    notificationService.createNotification(
        disbursement.getUserPlafond().getUser().getId(),
        "Dana Telah Dicairkan!",
        "Pencairan sebesar Rp "
            + disbursement.getAmount()
            + " telah berhasil diproses. "
            + "Total yang harus dibayar: Rp "
            + disbursement.getTotalAmount(),
        Notification.NotificationType.LOAN_DISBURSED,
        saved.getId());

    log.info(
        "Disbursement {} processed by Back Office: {}",
        disbursementId,
        backOfficeUser.getUsername());

    return mapToResponse(saved, saved.getUserPlafond());
  }

  // ========== BACK OFFICE: Cancel Disbursement ==========

  @Transactional
  public DisbursementResponse cancelDisbursement(
      Long backOfficeUserId, Long disbursementId, String reason) {
    Disbursement disbursement =
        disbursementRepository
            .findById(disbursementId)
            .orElseThrow(() -> new ResourceNotFoundException("Disbursement", "id", disbursementId));

    if (disbursement.getStatus() != Disbursement.DisbursementStatus.PENDING) {
      throw new IllegalStateException("Only PENDING disbursements can be cancelled");
    }

    // Return the reserved limit
    UserPlafond userPlafond = disbursement.getUserPlafond();
    userPlafond.setUsedAmount(userPlafond.getUsedAmount().subtract(disbursement.getAmount()));
    userPlafondRepository.save(userPlafond);

    disbursement.setStatus(Disbursement.DisbursementStatus.CANCELLED);
    disbursement.setNote(reason);

    Disbursement saved = disbursementRepository.save(disbursement);

    // Send notification
    notificationService.createNotification(
        userPlafond.getUser().getId(),
        "Pencairan Dibatalkan",
        "Pencairan sebesar Rp " + disbursement.getAmount() + " dibatalkan. Alasan: " + reason,
        Notification.NotificationType.LOAN_REJECTED,
        saved.getId());

    log.info("Disbursement {} cancelled. Reason: {}", disbursementId, reason);

    return mapToResponse(saved, userPlafond);
  }

  // ========== MAPPER ==========

  private DisbursementResponse mapToResponse(Disbursement d, UserPlafond up) {
    return DisbursementResponse.builder()
        .id(d.getId())
        .userPlafondId(up.getId())
        .plafondName(up.getPlafond() != null ? up.getPlafond().getName() : null)
        .customerUsername(up.getUser() != null ? up.getUser().getUsername() : null)
        .customerName(up.getUser() != null ? up.getUser().getFullName() : null)
        .bankName(up.getBankName())
        .accountNumber(up.getAccountNumber())
        .amount(d.getAmount())
        .interestRate(d.getInterestRate())
        .tenorMonth(d.getTenorMonth())
        .interestAmount(d.getInterestAmount())
        .totalAmount(d.getTotalAmount())
        .status(d.getStatus().name())
        .requestedAt(d.getRequestedAt())
        .disbursedAt(d.getDisbursedAt())
        .disbursedByUsername(d.getDisbursedBy() != null ? d.getDisbursedBy().getUsername() : null)
        .note(d.getNote())
        .requestLatitude(d.getRequestLatitude())
        .requestLongitude(d.getRequestLongitude())
        .remainingLimit(up.getAvailableLimit())
        .build();
  }
}
