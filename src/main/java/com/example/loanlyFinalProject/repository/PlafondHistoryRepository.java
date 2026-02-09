package com.example.loanlyFinalProject.repository;

import com.example.loanlyFinalProject.entity.PlafondHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlafondHistoryRepository extends JpaRepository<PlafondHistory, Long> {

  List<PlafondHistory> findByUserPlafondIdOrderByCreatedAtDesc(Long userPlafondId);

  // Find Marketing review entry for an application
  @org.springframework.data.jpa.repository.Query(
      "SELECT h FROM PlafondHistory h WHERE h.userPlafond.id = :applicationId "
          + "AND h.actionByRole = 'MARKETING' "
          + "AND h.newStatus = com.example.loanlyFinalProject.entity.UserPlafond$PlafondApplicationStatus.WAITING_APPROVAL "
          + "ORDER BY h.createdAt DESC")
  java.util.Optional<PlafondHistory> findMarketingReviewByApplicationId(
      @org.springframework.data.repository.query.Param("applicationId") Long applicationId);
}
