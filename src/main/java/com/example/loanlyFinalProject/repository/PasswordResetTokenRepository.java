package com.example.loanlyFinalProject.repository;

import com.example.loanlyFinalProject.entity.PasswordResetToken;
import com.example.loanlyFinalProject.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  Optional<PasswordResetToken> findByToken(String token);

  Optional<PasswordResetToken> findByTokenAndIsUsedFalse(String token);

  void deleteByUser(User user);
}
