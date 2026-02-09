package com.example.loanlyFinalProject.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.example.loanlyFinalProject.dto.request.PlafondRequest;
import com.example.loanlyFinalProject.dto.response.PlafondResponse;
import com.example.loanlyFinalProject.entity.Plafond;
import com.example.loanlyFinalProject.exception.DuplicateResourceException;
import com.example.loanlyFinalProject.exception.ResourceNotFoundException;
import com.example.loanlyFinalProject.repository.PlafondRepository;
import com.example.loanlyFinalProject.repository.UserPlafondRepository;
import com.example.loanlyFinalProject.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlafondService Unit Tests")
class PlafondServiceTest {

  @Mock private PlafondRepository plafondRepository;

  @Mock private UserPlafondRepository userPlafondRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private PlafondService plafondService;

  private Plafond testPlafond;

  @BeforeEach
  void setUp() {
    testPlafond =
        Plafond.builder()
            .id(1L)
            .name("Gold Plafond")
            .description("Premium loan plafond")
            .maxAmount(new BigDecimal("100000000"))
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();
  }

  @Test
  @DisplayName("Get All Active Plafonds - Should return list of active plafonds")
  void getAllActivePlafonds_ShouldReturnActivePlafonds() {
    // Arrange
    List<Plafond> plafonds = Arrays.asList(testPlafond);
    when(plafondRepository.findAllActive()).thenReturn(plafonds);

    // Act
    List<PlafondResponse> result = plafondService.getAllActivePlafonds();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Gold Plafond", result.get(0).getName());
  }

  @Test
  @DisplayName("Get Plafond By ID - Should return plafond when exists")
  void getPlafondById_ShouldReturnPlafond_WhenExists() {
    // Arrange
    when(plafondRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPlafond));

    // Act
    PlafondResponse result = plafondService.getPlafondById(1L);

    // Assert
    assertNotNull(result);
    assertEquals(1L, result.getId());
    assertEquals("Gold Plafond", result.getName());
  }

  @Test
  @DisplayName("Get Plafond By ID - Should throw exception when not found")
  void getPlafondById_ShouldThrowException_WhenNotFound() {
    // Arrange
    when(plafondRepository.findByIdNotDeleted(anyLong())).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> plafondService.getPlafondById(999L));
  }

  @Test
  @DisplayName("Create Plafond - Should create plafond successfully")
  void createPlafond_ShouldCreatePlafond_WhenValidRequest() {
    // Arrange
    PlafondRequest request =
        PlafondRequest.builder()
            .name("Silver Plafond")
            .description("Standard loan plafond")
            .maxAmount(new BigDecimal("50000000"))
            .isActive(true)
            .build();

    when(plafondRepository.existsByName("Silver Plafond")).thenReturn(false);
    when(plafondRepository.save(any(Plafond.class))).thenReturn(testPlafond);

    // Act
    PlafondResponse result = plafondService.createPlafond(request);

    // Assert
    assertNotNull(result);
    verify(plafondRepository).save(any(Plafond.class));
  }

  @Test
  @DisplayName("Create Plafond - Should throw exception when name exists")
  void createPlafond_ShouldThrowException_WhenNameExists() {
    // Arrange
    PlafondRequest request =
        PlafondRequest.builder()
            .name("Gold Plafond")
            .description("Duplicate plafond")
            .maxAmount(new BigDecimal("50000000"))
            .build();

    when(plafondRepository.existsByName("Gold Plafond")).thenReturn(true);

    // Act & Assert
    assertThrows(DuplicateResourceException.class, () -> plafondService.createPlafond(request));
    verify(plafondRepository, never()).save(any(Plafond.class));
  }

  @Test
  @DisplayName("Delete Plafond - Should soft delete plafond")
  void deletePlafond_ShouldSoftDelete_WhenPlafondExists() {
    // Arrange
    when(plafondRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPlafond));
    when(plafondRepository.save(any(Plafond.class))).thenReturn(testPlafond);

    // Act
    plafondService.deletePlafond(1L);

    // Assert
    verify(plafondRepository).save(any(Plafond.class));
    assertNotNull(testPlafond.getDeletedAt());
  }
}
