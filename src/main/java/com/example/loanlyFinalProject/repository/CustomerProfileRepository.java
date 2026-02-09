package com.example.loanlyFinalProject.repository;

import com.example.loanlyFinalProject.entity.CustomerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

  Optional<CustomerProfile> findByUserId(Long userId);
}
