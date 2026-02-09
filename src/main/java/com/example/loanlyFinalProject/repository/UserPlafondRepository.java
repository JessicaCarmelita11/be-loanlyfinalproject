package com.example.loanlyFinalProject.repository;

import com.example.loanlyFinalProject.entity.UserPlafond;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPlafondRepository extends JpaRepository<UserPlafond, Long> {

  List<UserPlafond> findByUserId(Long userId);

  List<UserPlafond> findByPlafondId(Long plafondId);

  Optional<UserPlafond> findByUserIdAndPlafondId(Long userId, Long plafondId);

  boolean existsByUserIdAndPlafondId(Long userId, Long plafondId);

  @Query("SELECT up FROM UserPlafond up JOIN FETCH up.plafond WHERE up.user.id = :userId")
  List<UserPlafond> findByUserIdWithPlafond(@Param("userId") Long userId);

  // ========== NEW: For 2-Step Loan Flow ==========

  // Find by status (for Marketing and Branch Manager)
  List<UserPlafond> findByStatusOrderByRegisteredAtAsc(UserPlafond.PlafondApplicationStatus status);

  // Find pending review (for Marketing)
  @Query(
      "SELECT up FROM UserPlafond up WHERE up.status = 'PENDING_REVIEW' ORDER BY up.registeredAt ASC")
  List<UserPlafond> findAllPendingReview();

  // Find waiting approval (for Branch Manager)
  @Query(
      "SELECT up FROM UserPlafond up WHERE up.status = 'WAITING_APPROVAL' ORDER BY up.registeredAt ASC")
  List<UserPlafond> findAllWaitingApproval();

  // Find approved by user (for Customer to see their approved credit lines)
  @Query("SELECT up FROM UserPlafond up WHERE up.user.id = :userId AND up.status = 'APPROVED'")
  List<UserPlafond> findApprovedByUserId(@Param("userId") Long userId);

  // Find with details
  @Query(
      "SELECT up FROM UserPlafond up "
          + "LEFT JOIN FETCH up.plafond "
          + "LEFT JOIN FETCH up.documents "
          + "WHERE up.id = :id")
  Optional<UserPlafond> findByIdWithDetails(@Param("id") Long id);

  // Check if user already has pending/approved for same plafond
  @Query(
      "SELECT COUNT(up) > 0 FROM UserPlafond up "
          + "WHERE up.user.id = :userId AND up.plafond.id = :plafondId "
          + "AND up.status IN ('PENDING_REVIEW', 'WAITING_APPROVAL', 'APPROVED')")
  boolean existsActiveByUserIdAndPlafondId(
      @Param("userId") Long userId, @Param("plafondId") Long plafondId);

  // Find all approved plafonds (for admin customer list)
  @Query(
      "SELECT up FROM UserPlafond up "
          + "LEFT JOIN FETCH up.user "
          + "LEFT JOIN FETCH up.plafond "
          + "WHERE up.status = 'APPROVED' "
          + "ORDER BY up.approvedAt DESC")
  List<UserPlafond> findAllApproved();

  // ========== CREDIT ELIGIBILITY: Tier-Up Logic ==========

  // Find user's current APPROVED plafond with remaining limit
  @Query(
      "SELECT up FROM UserPlafond up "
          + "JOIN FETCH up.plafond "
          + "WHERE up.user.id = :userId "
          + "AND up.status = 'APPROVED' "
          + "AND (up.approvedLimit - COALESCE(up.usedAmount, 0)) > 0 "
          + "ORDER BY up.approvedAt DESC")
  List<UserPlafond> findActiveWithRemainingLimit(@Param("userId") Long userId);

  // Find user's highest tier plafond ever approved (for tier-up requirement)
  @Query(
      "SELECT up FROM UserPlafond up "
          + "JOIN FETCH up.plafond p "
          + "WHERE up.user.id = :userId "
          + "AND up.status = 'APPROVED' "
          + "ORDER BY p.maxAmount DESC")
  List<UserPlafond> findHighestApprovedPlafond(@Param("userId") Long userId);

  // Check if user has any pending application
  @Query(
      "SELECT COUNT(up) > 0 FROM UserPlafond up "
          + "WHERE up.user.id = :userId "
          + "AND up.status IN ('PENDING_REVIEW', 'WAITING_APPROVAL')")
  boolean hasPendingApplication(@Param("userId") Long userId);
}
