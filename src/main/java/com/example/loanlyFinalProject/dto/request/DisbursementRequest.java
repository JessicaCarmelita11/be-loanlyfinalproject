package com.example.loanlyFinalProject.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisbursementRequest {

  // Pilihan tenor yang valid (dalam bulan)
  public static final List<Integer> VALID_TENORS = Arrays.asList(1, 3, 6, 9, 12, 15, 18, 21, 24);

  @NotNull(message = "User Plafond ID is required")
  private Long userPlafondId;

  @NotNull(message = "Amount is required")
  @DecimalMin(value = "0", inclusive = false, message = "Amount must be positive")
  private BigDecimal amount;

  @NotNull(message = "Tenor is required")
  @Min(value = 1, message = "Minimum tenor is 1 month")
  @Max(value = 24, message = "Maximum tenor is 24 months")
  private Integer tenorMonth;

  /** Validate if tenor is one of the allowed values */
  public boolean isValidTenor() {
    return tenorMonth != null && VALID_TENORS.contains(tenorMonth);
  }

  // Optional location data from Android device
  private BigDecimal latitude;
  private BigDecimal longitude;
}
