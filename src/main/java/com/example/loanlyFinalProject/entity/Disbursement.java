package com.example.loanlyFinalProject.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "disbursements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disbursement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_plafond_id", nullable = false)
  @JsonIgnore
  private UserPlafond userPlafond;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal interestRate;

  @Column(name = "tenor_month", nullable = false)
  private Integer tenorMonth;

  @Column(name = "interest_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal interestAmount;

  @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalAmount;

  @Column(nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private DisbursementStatus status = DisbursementStatus.PENDING;

  @Column(name = "requested_at", nullable = false, updatable = false)
  private LocalDateTime requestedAt;

  @Column(name = "disbursed_at")
  private LocalDateTime disbursedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "disbursed_by")
  @JsonIgnore
  private User disbursedBy;

  @Column(length = 255)
  private String note;

  // ========== LOCATION TRACKING ==========

  @Column(name = "request_latitude", precision = 10, scale = 7)
  private BigDecimal requestLatitude;

  @Column(name = "request_longitude", precision = 10, scale = 7)
  private BigDecimal requestLongitude;

  @PrePersist
  protected void onCreate() {
    requestedAt = LocalDateTime.now();
    if (status == null) {
      status = DisbursementStatus.PENDING;
    }
  }

  public enum DisbursementStatus {
    PENDING, // Waiting for Back Office to process
    DISBURSED, // Successfully disbursed
    CANCELLED // Cancelled
  }
}
