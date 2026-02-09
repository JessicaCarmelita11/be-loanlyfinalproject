package com.example.loanlyFinalProject.service;

import com.example.loanlyFinalProject.dto.request.PlafondRequest;
import com.example.loanlyFinalProject.dto.response.PlafondResponse;
import com.example.loanlyFinalProject.entity.Plafond;
import com.example.loanlyFinalProject.entity.User;
import com.example.loanlyFinalProject.entity.UserPlafond;
import com.example.loanlyFinalProject.exception.DuplicateResourceException;
import com.example.loanlyFinalProject.exception.ResourceNotFoundException;
import com.example.loanlyFinalProject.repository.PlafondRepository;
import com.example.loanlyFinalProject.repository.UserPlafondRepository;
import com.example.loanlyFinalProject.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlafondService {

  private final PlafondRepository plafondRepository;
  private final UserPlafondRepository userPlafondRepository;
  private final UserRepository userRepository;

  // ========== PUBLIC API (No Auth Required) ==========

  @Cacheable(value = "plafonds", key = "'all'")
  public List<PlafondResponse> getAllActivePlafonds() {
    log.info("Cache MISS - Fetching all active plafonds from database");
    return plafondRepository.findAllActive().stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Cacheable(value = "plafonds", key = "#id")
  public PlafondResponse getPlafondById(Long id) {
    log.info("Cache MISS - Fetching plafond {} from database", id);
    Plafond plafond =
        plafondRepository
            .findByIdNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plafond", "id", id));
    return mapToResponse(plafond);
  }

  // ========== ADMIN CRUD ==========

  public Page<PlafondResponse> getAllPlafonds(Pageable pageable) {
    return plafondRepository.findAllNotDeleted(pageable).map(this::mapToResponse);
  }

  public Page<PlafondResponse> searchPlafonds(String name, Pageable pageable) {
    return plafondRepository.searchByName(name, pageable).map(this::mapToResponse);
  }

  @Transactional
  @CacheEvict(value = "plafonds", allEntries = true)
  public PlafondResponse createPlafond(PlafondRequest request) {
    if (plafondRepository.existsByName(request.getName())) {
      throw new DuplicateResourceException(
          "Plafond with name '" + request.getName() + "' already exists");
    }

    Plafond plafond =
        Plafond.builder()
            .name(request.getName())
            .description(request.getDescription())
            .maxAmount(request.getMaxAmount())
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();

    Plafond savedPlafond = plafondRepository.save(plafond);
    log.info("Created plafond: {}", savedPlafond.getName());

    return mapToResponse(savedPlafond);
  }

  @Transactional
  @CacheEvict(value = "plafonds", allEntries = true)
  public PlafondResponse updatePlafond(Long id, PlafondRequest request) {
    Plafond plafond =
        plafondRepository
            .findByIdNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plafond", "id", id));

    // Check if name is being changed and if new name already exists
    if (!plafond.getName().equals(request.getName())
        && plafondRepository.existsByName(request.getName())) {
      throw new DuplicateResourceException(
          "Plafond with name '" + request.getName() + "' already exists");
    }

    plafond.setName(request.getName());
    plafond.setDescription(request.getDescription());
    plafond.setMaxAmount(request.getMaxAmount());
    plafond.setIsActive(request.getIsActive());

    Plafond updatedPlafond = plafondRepository.save(plafond);
    log.info("Updated plafond: {}", updatedPlafond.getName());

    return mapToResponse(updatedPlafond);
  }

  @Transactional
  @CacheEvict(value = "plafonds", allEntries = true)
  public void deletePlafond(Long id) {
    Plafond plafond =
        plafondRepository
            .findByIdNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plafond", "id", id));

    // Soft delete
    plafond.setDeletedAt(LocalDateTime.now());
    plafondRepository.save(plafond);

    log.info("Soft deleted plafond: {}", plafond.getName());
  }

  // ========== USER PLAFOND REGISTRATION ==========

  @Transactional
  public void registerUserToPlafond(Long userId, Long plafondId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

    Plafond plafond =
        plafondRepository
            .findByIdNotDeleted(plafondId)
            .orElseThrow(() -> new ResourceNotFoundException("Plafond", "id", plafondId));

    if (!plafond.getIsActive()) {
      throw new IllegalStateException("Cannot register to inactive plafond");
    }

    if (userPlafondRepository.existsByUserIdAndPlafondId(userId, plafondId)) {
      throw new DuplicateResourceException("User already registered to this plafond");
    }

    UserPlafond userPlafond = UserPlafond.builder().user(user).plafond(plafond).build();

    userPlafondRepository.save(userPlafond);
    log.info("User {} registered to plafond {}", user.getUsername(), plafond.getName());
  }

  public List<PlafondResponse> getUserPlafonds(Long userId) {
    return userPlafondRepository.findByUserIdWithPlafond(userId).stream()
        .map(up -> mapToResponse(up.getPlafond()))
        .collect(Collectors.toList());
  }

  // ========== MAPPER ==========

  private PlafondResponse mapToResponse(Plafond plafond) {
    return PlafondResponse.builder()
        .id(plafond.getId())
        .name(plafond.getName())
        .description(plafond.getDescription())
        .maxAmount(plafond.getMaxAmount())
        .isActive(plafond.getIsActive())
        .createdAt(plafond.getCreatedAt())
        .build();
  }
}
