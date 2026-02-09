package com.example.loanlyFinalProject.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "user_plafonds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(hidden = true)
public class UserPlafond {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnore
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plafond_id", nullable = false)
  private Plafond plafond;

  @Column(name = "registered_at", nullable = false, updatable = false)
  private LocalDateTime registeredAt;

  // ========== APPROVAL WORKFLOW ==========

  @Column(nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private PlafondApplicationStatus status = PlafondApplicationStatus.PENDING_REVIEW;

  @Column(name = "approved_limit", precision = 18, scale = 2)
  private BigDecimal approvedLimit;

  @Column(name = "used_amount", precision = 18, scale = 2)
  @Builder.Default
  private BigDecimal usedAmount = BigDecimal.ZERO;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by")
  @JsonIgnore
  private User reviewedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by")
  @JsonIgnore
  private User approvedBy;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Column(name = "rejection_note")
  private String rejectionNote;

  // ========== APPLICANT DETAILS ==========

  @Column(length = 20)
  private String nik;

  @Column(name = "birth_place", length = 100)
  private String birthPlace;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(name = "marital_status", length = 20)
  private String maritalStatus;

  @Column(length = 100)
  private String occupation;

  @Column(name = "monthly_income", precision = 18, scale = 2)
  private BigDecimal monthlyIncome;

  @Column(length = 20)
  private String phone;

  @Column(length = 25)
  private String npwp;

  @Column(name = "bank_name", length = 50)
  private String bankName;

  @Column(name = "account_number", length = 30)
  private String accountNumber;

  // ========== LOCATION TRACKING ==========

  @Column(name = "application_latitude", precision = 10, scale = 7)
  private BigDecimal applicationLatitude;

  @Column(name = "application_longitude", precision = 10, scale = 7)
  private BigDecimal applicationLongitude;

  // ========== RELATIONSHIPS ==========

  @OneToMany(mappedBy = "userPlafond", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private List<PlafondDocument> documents = new ArrayList<>();

  @OneToMany(mappedBy = "userPlafond", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private List<Disbursement> disbursements = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    registeredAt = LocalDateTime.now();
    if (status == null) {
      status = PlafondApplicationStatus.PENDING_REVIEW;
    }
    if (usedAmount == null) {
      usedAmount = BigDecimal.ZERO;
    }
  }

  // ========== HELPER METHODS ==========

  public BigDecimal getAvailableLimit() {
    if (approvedLimit == null) return BigDecimal.ZERO;
    System.out.println("Approved Limit: " + approvedLimit);
    System.out.println("Used Amount: " + usedAmount);
    return approvedLimit.subtract(usedAmount != null ? usedAmount : BigDecimal.ZERO);
  }

  public boolean canDisburse(BigDecimal amount) {
    return status == PlafondApplicationStatus.APPROVED
        && getAvailableLimit().compareTo(amount) >= 0;
  }

  public enum PlafondApplicationStatus {
    PENDING_REVIEW, // Waiting for Marketing review
    WAITING_APPROVAL, // Marketing approved, waiting for Branch Manager
    APPROVED, // Fully approved, can disburse
    REJECTED // Rejected at any stage
  }
}
