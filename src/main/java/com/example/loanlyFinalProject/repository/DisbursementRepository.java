package com.example.loanlyFinalProject.repository;

import com.example.loanlyFinalProject.entity.Disbursement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DisbursementRepository extends JpaRepository<Disbursement, Long> {

  // Find by user plafond
  List<Disbursement> findByUserPlafondIdOrderByRequestedAtDesc(Long userPlafondId);

  // Find by status
  List<Disbursement> findByStatusOrderByRequestedAtAsc(Disbursement.DisbursementStatus status);

  // Find pending disbursements for Back Office
  @Query("SELECT d FROM Disbursement d WHERE d.status = 'PENDING' ORDER BY d.requestedAt ASC")
  List<Disbursement> findAllPending();

  // Find by user (through userPlafond)
  @Query(
      "SELECT d FROM Disbursement d WHERE d.userPlafond.user.id = :userId ORDER BY d.requestedAt DESC")
  List<Disbursement> findByUserId(@Param("userId") Long userId);

  // Count pending
  long countByStatus(Disbursement.DisbursementStatus status);
}
