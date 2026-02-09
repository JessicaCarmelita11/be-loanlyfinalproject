package com.example.loanlyFinalProject.repository;

import com.example.loanlyFinalProject.entity.Permission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

  Optional<Permission> findByCode(String code);

  boolean existsByCode(String code);
}
