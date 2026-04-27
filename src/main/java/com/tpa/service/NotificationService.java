package com.tpa.service;

import com.tpa.dto.response.NotificationResponse;
import com.tpa.entity.Notification;
import com.tpa.entity.User;
import com.tpa.enums.UserRole;
import com.tpa.repository.NotificationRepository;
import com.tpa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;

    /** Saves a notification for a specific user. */
    @Transactional
    public void createNotification(User user, String title, String message, String targetUrl) {
        Notification n = Notification.builder()
                .user(user).title(title).message(message).targetUrl(targetUrl).build();
        notificationRepository.save(n);
        log.info("Notification saved for user {}: {}", user.getEmail(), title);
    }

    /**
     * Saves a notification for ALL admin users (role = FMG_ADMIN).
     * Called asynchronously so it never blocks the HTTP request.
     */
    @Async("taskExecutor")
    @Transactional
    public void notifyAllAdmins(String title, String message, String targetUrl) {
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> UserRole.FMG_ADMIN.equals(u.getUserRole()))
                .collect(Collectors.toList());
        admins.forEach(admin -> createNotification(admin, title, message, targetUrl));
        log.info("Admin notification queued for {} admins: {}", admins.size(), title);
    }

    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public long countUnread(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().filter(n -> !n.isRead()).count();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void markOneAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    private NotificationResponse toDto(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .targetUrl(n.getTargetUrl())
                .build();
    }
}
