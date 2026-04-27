package com.tpa.service.impl;

import com.tpa.dto.response.NotificationResponse;
import com.tpa.entity.Notification;
import com.tpa.entity.User;
import com.tpa.enums.UserRole;
import com.tpa.repository.NotificationRepository;
import com.tpa.repository.UserRepository;
import com.tpa.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("admin@tpa.com").userRole(UserRole.FMG_ADMIN).build();
        testNotification = Notification.builder()
                .id(100L)
                .user(testUser)
                .title("Test Title")
                .message("Test Message")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .targetUrl("/admin/claims/1")
                .build();
    }

    @Test
    @DisplayName("TC-NOTIF-01: Create Notification successfully")
    void createNotification_success() {
        notificationService.createNotification(testUser, "Title", "Message", "/url");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("TC-NOTIF-02: Notify All Admins successfully")
    void notifyAllAdmins_success() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        notificationService.notifyAllAdmins("Title", "Message", "/url");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("TC-NOTIF-03: Get User Notifications successfully")
    void getUserNotifications_success() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testNotification));
        List<NotificationResponse> result = notificationService.getUserNotifications(1L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Title");
    }

    @Test
    @DisplayName("TC-NOTIF-04: Count Unread successfully")
    void countUnread_success() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testNotification));
        long count = notificationService.countUnread(1L);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("TC-NOTIF-05: Mark All As Read successfully")
    void markAllAsRead_success() {
        notificationService.markAllAsRead(1L);
        verify(notificationRepository).markAllAsReadByUserId(1L);
    }

    @Test
    @DisplayName("TC-NOTIF-06: Mark One As Read successfully")
    void markOneAsRead_success() {
        when(notificationRepository.findById(100L)).thenReturn(Optional.of(testNotification));
        notificationService.markOneAsRead(100L, 1L);
        assertThat(testNotification.isRead()).isTrue();
        verify(notificationRepository).save(testNotification);
    }
}
