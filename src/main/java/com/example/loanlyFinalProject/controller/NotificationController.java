package com.example.loanlyFinalProject.controller;

import com.example.loanlyFinalProject.dto.response.ApiResponse;
import com.example.loanlyFinalProject.dto.response.NotificationResponse;
import com.example.loanlyFinalProject.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  @Operation(
      summary = "Get all notifications",
      description = "Returns all notifications for the authenticated user")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
      @RequestAttribute("userId") Long userId) {
    List<NotificationResponse> notifications = notificationService.getUserNotifications(userId);
    return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notifications));
  }

  @GetMapping("/unread")
  @Operation(
      summary = "Get unread notifications",
      description = "Returns only unread notifications")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
      @RequestAttribute("userId") Long userId) {
    List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId);
    return ResponseEntity.ok(ApiResponse.success("Unread notifications retrieved", notifications));
  }

  @GetMapping("/count")
  @Operation(summary = "Get unread count", description = "Returns count of unread notifications")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
      @RequestAttribute("userId") Long userId) {
    long count = notificationService.getUnreadCount(userId);
    return ResponseEntity.ok(ApiResponse.success("Count retrieved", Map.of("unreadCount", count)));
  }

  @PutMapping("/{notificationId}/read")
  @Operation(
      summary = "Mark notification as read",
      description = "Marks a specific notification as read")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<Object>> markAsRead(@PathVariable Long notificationId) {
    notificationService.markAsRead(notificationId);
    return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
  }

  @PutMapping("/read-all")
  @Operation(
      summary = "Mark all as read",
      description = "Marks all notifications as read for the user")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
      @RequestAttribute("userId") Long userId) {
    int count = notificationService.markAllAsRead(userId);
    return ResponseEntity.ok(
        ApiResponse.success("All notifications marked as read", Map.of("markedCount", count)));
  }
}
