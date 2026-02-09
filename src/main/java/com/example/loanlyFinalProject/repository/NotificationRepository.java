package com.example.loanlyFinalProject.repository;

import com.example.loanlyFinalProject.entity.Notification;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  // Find by user, ordered by newest first
  List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

  // Find by user with pagination
  Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  // Find unread by user
  List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

  // Count unread by user
  long countByUserIdAndIsReadFalse(Long userId);

  // Mark all as read for user
  @Modifying
  @Query(
      "UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.isRead = false")
  int markAllAsReadByUserId(@Param("userId") Long userId);
}
