package com.example.loanlyFinalProject.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlafondApplicationRequest {

  @NotNull(message = "Plafond ID is required")
  private Long plafondId;

  @NotBlank(message = "NIK is required")
  @Size(min = 16, max = 16, message = "NIK must be 16 digits")
  private String nik;

  @NotBlank(message = "Birth place is required")
  private String birthPlace;

  @NotNull(message = "Birth date is required")
  private LocalDate birthDate;

  private String maritalStatus;

  @NotBlank(message = "Occupation is required")
  private String occupation;

  @NotNull(message = "Monthly income is required")
  @DecimalMin(value = "0", inclusive = false, message = "Monthly income must be positive")
  private BigDecimal monthlyIncome;

  @NotBlank(message = "Phone is required")
  @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid phone number format")
  private String phone;

  private String npwp;

  @NotBlank(message = "Bank name is required")
  private String bankName;

  @NotBlank(message = "Account number is required")
  private String accountNumber; // No Rekening

  // Optional location data from Android device
  private BigDecimal latitude;
  private BigDecimal longitude;
}
