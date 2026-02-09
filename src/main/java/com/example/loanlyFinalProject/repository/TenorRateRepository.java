package com.example.loanlyFinalProject.repository;

import com.example.loanlyFinalProject.entity.TenorRate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TenorRateRepository extends JpaRepository<TenorRate, Long> {

  // Find by plafond and tenor (for disbursement calculation)
  @Query(
      "SELECT tr FROM TenorRate tr WHERE tr.plafond.id = :plafondId AND tr.tenorMonth = :tenorMonth AND tr.isActive = true")
  Optional<TenorRate> findActiveByPlafondIdAndTenorMonth(
      @Param("plafondId") Long plafondId, @Param("tenorMonth") Integer tenorMonth);

  // Get all rates for a specific plafond
  @Query(
      "SELECT tr FROM TenorRate tr WHERE tr.plafond.id = :plafondId AND tr.isActive = true ORDER BY tr.tenorMonth ASC")
  List<TenorRate> findAllActiveByPlafondId(@Param("plafondId") Long plafondId);

  // Get all active rates (for admin view)
  @Query(
      "SELECT tr FROM TenorRate tr WHERE tr.isActive = true ORDER BY tr.plafond.id ASC, tr.tenorMonth ASC")
  List<TenorRate> findAllActive();

  // Check if rate exists for plafond + tenor combination
  boolean existsByPlafondIdAndTenorMonth(Long plafondId, Integer tenorMonth);

  // Legacy methods (keeping for backward compatibility during migration)
  Optional<TenorRate> findByTenorMonth(Integer tenorMonth);

  @Query("SELECT tr FROM TenorRate tr WHERE tr.tenorMonth = :tenorMonth AND tr.isActive = true")
  Optional<TenorRate> findActiveByTenorMonth(@Param("tenorMonth") Integer tenorMonth);

  boolean existsByTenorMonth(Integer tenorMonth);
}
