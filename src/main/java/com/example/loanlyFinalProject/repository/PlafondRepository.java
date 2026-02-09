package com.example.loanlyFinalProject.repository;

import com.example.loanlyFinalProject.entity.Plafond;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlafondRepository extends JpaRepository<Plafond, Long> {

  // Find all active plafonds (not deleted)
  @Query("SELECT p FROM Plafond p WHERE p.deletedAt IS NULL AND p.isActive = true")
  List<Plafond> findAllActive();

  // Find all with pagination (not deleted)
  @Query("SELECT p FROM Plafond p WHERE p.deletedAt IS NULL")
  Page<Plafond> findAllNotDeleted(Pageable pageable);

  // Find by id and not deleted
  @Query("SELECT p FROM Plafond p WHERE p.id = :id AND p.deletedAt IS NULL")
  Optional<Plafond> findByIdNotDeleted(@Param("id") Long id);

  // Search by name
  @Query(
      "SELECT p FROM Plafond p WHERE p.deletedAt IS NULL AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
  Page<Plafond> searchByName(@Param("name") String name, Pageable pageable);

  boolean existsByName(String name);
}
