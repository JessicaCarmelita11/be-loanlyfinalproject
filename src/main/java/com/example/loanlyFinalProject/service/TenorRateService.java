package com.example.loanlyFinalProject.service;

import com.example.loanlyFinalProject.dto.request.TenorRateRequest;
import com.example.loanlyFinalProject.dto.response.TenorRateResponse;
import com.example.loanlyFinalProject.entity.Plafond;
import com.example.loanlyFinalProject.entity.TenorRate;
import com.example.loanlyFinalProject.exception.DuplicateResourceException;
import com.example.loanlyFinalProject.exception.ResourceNotFoundException;
import com.example.loanlyFinalProject.repository.PlafondRepository;
import com.example.loanlyFinalProject.repository.TenorRateRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenorRateService {

  private final TenorRateRepository tenorRateRepository;
  private final PlafondRepository plafondRepository;

  // Get all rates grouped by plafond
  public Map<String, List<TenorRateResponse>> getAllRatesGroupedByPlafond() {
    List<TenorRate> rates = tenorRateRepository.findAllActive();
    return rates.stream()
        .map(this::mapToResponse)
        .collect(Collectors.groupingBy(TenorRateResponse::getPlafondName));
  }

  // Get all rates (flat list)
  public List<TenorRateResponse> getAllRates() {
    return tenorRateRepository.findAllActive().stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  // Get rates for specific plafond
  @Cacheable(value = "tenorRates", key = "#plafondId")
  public List<TenorRateResponse> getRatesByPlafondId(Long plafondId) {
    log.info("Cache MISS - Fetching tenor rates for plafond {} from database", plafondId);
    Plafond plafond =
        plafondRepository
            .findById(plafondId)
            .orElseThrow(() -> new ResourceNotFoundException("Plafond", "id", plafondId));

    return tenorRateRepository.findAllActiveByPlafondId(plafondId).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  // Create new rate
  @Transactional
  @CacheEvict(value = "tenorRates", allEntries = true)
  public TenorRateResponse createRate(TenorRateRequest request) {
    Plafond plafond =
        plafondRepository
            .findById(request.getPlafondId())
            .orElseThrow(
                () -> new ResourceNotFoundException("Plafond", "id", request.getPlafondId()));

    // Check for duplicate
    if (tenorRateRepository.existsByPlafondIdAndTenorMonth(
        request.getPlafondId(), request.getTenorMonth())) {
      throw new DuplicateResourceException(
          "Rate for Plafond '"
              + plafond.getName()
              + "' with tenor "
              + request.getTenorMonth()
              + " months already exists");
    }

    TenorRate rate =
        TenorRate.builder()
            .plafond(plafond)
            .tenorMonth(request.getTenorMonth())
            .interestRate(request.getInterestRate())
            .description(request.getDescription())
            .isActive(true)
            .build();

    TenorRate saved = tenorRateRepository.save(rate);
    log.info(
        "Created tenor rate: {} - {} months -> {}%",
        plafond.getName(), request.getTenorMonth(), request.getInterestRate());

    return mapToResponse(saved);
  }

  // Update existing rate
  @Transactional
  @CacheEvict(value = "tenorRates", allEntries = true)
  public TenorRateResponse updateRate(Long rateId, TenorRateRequest request) {
    TenorRate rate =
        tenorRateRepository
            .findById(rateId)
            .orElseThrow(() -> new ResourceNotFoundException("TenorRate", "id", rateId));

    rate.setInterestRate(request.getInterestRate());
    if (request.getDescription() != null) {
      rate.setDescription(request.getDescription());
    }

    TenorRate saved = tenorRateRepository.save(rate);
    log.info("Updated tenor rate ID {}: {}%", rateId, request.getInterestRate());

    return mapToResponse(saved);
  }

  // Delete rate (soft delete)
  @Transactional
  @CacheEvict(value = "tenorRates", allEntries = true)
  public void deleteRate(Long rateId) {
    TenorRate rate =
        tenorRateRepository
            .findById(rateId)
            .orElseThrow(() -> new ResourceNotFoundException("TenorRate", "id", rateId));

    rate.setIsActive(false);
    tenorRateRepository.save(rate);
    log.info("Deactivated tenor rate ID {}", rateId);
  }

  // Mapper
  private TenorRateResponse mapToResponse(TenorRate rate) {
    return TenorRateResponse.builder()
        .id(rate.getId())
        .plafondId(rate.getPlafond().getId())
        .plafondName(rate.getPlafond().getName())
        .tenorMonth(rate.getTenorMonth())
        .interestRate(rate.getInterestRate())
        .description(rate.getDescription())
        .isActive(rate.getIsActive())
        .build();
  }
}
