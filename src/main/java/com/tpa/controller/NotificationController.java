package com.tpa.controller;

import com.tpa.dto.response.NotificationResponse;
import com.tpa.entity.User;
import com.tpa.repository.UserRepository;
import com.tpa.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository      userRepository;

    private User getUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /** GET /api/v1/notifications — fetch all notifications for current user */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(Principal principal) {
        User user = getUser(principal);
        return ResponseEntity.ok(notificationService.getUserNotifications(user.getId()));
    }

    /** GET /api/v1/notifications/unread-count */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Principal principal) {
        User user = getUser(principal);
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(user.getId())));
    }

    /** POST /api/v1/notifications/mark-read — mark all as read */
    @PostMapping("/mark-read")
    public ResponseEntity<Void> markAllAsRead(Principal principal) {
        notificationService.markAllAsRead(getUser(principal).getId());
        return ResponseEntity.ok().build();
    }

    /** PATCH /api/v1/notifications/{id}/read — mark single as read */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markOneAsRead(@PathVariable Long id, Principal principal) {
        notificationService.markOneAsRead(id, getUser(principal).getId());
        return ResponseEntity.ok().build();
    }
}
