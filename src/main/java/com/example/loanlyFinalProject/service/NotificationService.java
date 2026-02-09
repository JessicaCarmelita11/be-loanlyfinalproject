package com.example.loanlyFinalProject.service;

import com.example.loanlyFinalProject.dto.response.NotificationResponse;
import com.example.loanlyFinalProject.entity.Notification;
import com.example.loanlyFinalProject.entity.User;
import com.example.loanlyFinalProject.exception.ResourceNotFoundException;
import com.example.loanlyFinalProject.repository.NotificationRepository;
import com.example.loanlyFinalProject.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;

  // ========== Get Notifications ==========

  public List<NotificationResponse> getUserNotifications(Long userId) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  public List<NotificationResponse> getUnreadNotifications(Long userId) {
    return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  public long getUnreadCount(Long userId) {
    return notificationRepository.countByUserIdAndIsReadFalse(userId);
  }

  // ========== Mark as Read ==========

  @Transactional
  public void markAsRead(Long notificationId) {
    Notification notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

    notification.setIsRead(true);
    notification.setReadAt(LocalDateTime.now());
    notificationRepository.save(notification);
  }

  @Transactional
  public int markAllAsRead(Long userId) {
    return notificationRepository.markAllAsReadByUserId(userId);
  }

  // ========== Create Notifications (used by other services) ==========

  @Transactional
  public void createNotification(
      Long userId,
      String title,
      String message,
      Notification.NotificationType type,
      Long referenceId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

    Notification notification =
        Notification.builder()
            .user(user)
            .title(title)
            .message(message)
            .type(type)
            .referenceId(referenceId)
            .build();

    notificationRepository.save(notification);
    log.info("Notification created for user {}: {}", userId, title);

    // Send FCM push notification if user has FCM token
    sendFcmNotification(user, title, message, type, referenceId);
  }

  // ========== Push Notification ==========

  private void sendFcmNotification(
      User user, String title, String body, Notification.NotificationType type, Long referenceId) {
    if (user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
      return;
    }

    try {
      com.google.firebase.messaging.Notification notification =
          com.google.firebase.messaging.Notification.builder()
              .setTitle(title)
              .setBody(body)
              .build();

      com.google.firebase.messaging.Message.Builder messageBuilder =
          com.google.firebase.messaging.Message.builder()
              .setToken(user.getFcmToken())
              .setNotification(notification)
              .putData("type", type.name())
              .putData("click_action", "FLUTTER_NOTIFICATION_CLICK");

      if (referenceId != null) {
        messageBuilder.putData("referenceId", referenceId.toString());
      }

      com.google.firebase.messaging.FirebaseMessaging.getInstance().send(messageBuilder.build());
      log.info("FCM Notification sent to user: {}", user.getUsername());
    } catch (Exception e) {
      log.error("Failed to send FCM notification to user: {}", user.getUsername(), e);
    }
  }

  // ========== Loan Notification Helpers ==========

  public void notifyLoanSubmitted(Long userId, Long loanId) {
    createNotification(
        userId,
        "Pengajuan Pinjaman Diterima",
        "Pengajuan pinjaman Anda sedang dalam proses review oleh tim Marketing.",
        Notification.NotificationType.LOAN_SUBMITTED,
        loanId);
  }

  public void notifyLoanReviewed(Long userId, Long loanId, boolean approved) {
    if (approved) {
      createNotification(
          userId,
          "Pengajuan Pinjaman Disetujui Marketing",
          "Selamat! Pengajuan pinjaman Anda telah disetujui dan sedang menunggu persetujuan Branch Manager.",
          Notification.NotificationType.LOAN_REVIEWED,
          loanId);
    } else {
      createNotification(
          userId,
          "Pengajuan Pinjaman Ditolak",
          "Mohon maaf, pengajuan pinjaman Anda tidak memenuhi kriteria.",
          Notification.NotificationType.LOAN_REJECTED,
          loanId);
    }
  }

  public void notifyLoanApproved(Long userId, Long loanId, boolean approved) {
    if (approved) {
      createNotification(
          userId,
          "Pengajuan Pinjaman Disetujui",
          "Selamat! Pinjaman Anda telah disetujui dan sedang diproses untuk pencairan.",
          Notification.NotificationType.LOAN_APPROVED,
          loanId);
    } else {
      createNotification(
          userId,
          "Pengajuan Pinjaman Ditolak",
          "Mohon maaf, pengajuan pinjaman Anda tidak disetujui oleh Branch Manager.",
          Notification.NotificationType.LOAN_REJECTED,
          loanId);
    }
  }

  public void notifyLoanDisbursed(Long userId, Long loanId) {
    createNotification(
        userId,
        "Pinjaman Telah Dicairkan",
        "Selamat! Dana pinjaman Anda telah dicairkan. Silakan cek rekening Anda.",
        Notification.NotificationType.LOAN_DISBURSED,
        loanId);
  }

  // ========== Mapper ==========

  private NotificationResponse mapToResponse(Notification notification) {
    return NotificationResponse.builder()
        .id(notification.getId())
        .title(notification.getTitle())
        .message(notification.getMessage())
        .type(notification.getType().name())
        .referenceId(notification.getReferenceId())
        .isRead(notification.getIsRead())
        .createdAt(notification.getCreatedAt())
        .readAt(notification.getReadAt())
        .build();
  }
}
