package com.example.loanlyFinalProject.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlafondRequest {

  @NotBlank(message = "Plafond name is required")
  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @Size(max = 255, message = "Description must not exceed 255 characters")
  private String description;

  @NotNull(message = "Max amount is required")
  @DecimalMin(value = "0.01", message = "Max amount must be greater than 0")
  private BigDecimal maxAmount;

  private Boolean isActive = true;
}
