package com.tpa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpa.dto.response.NotificationResponse;
import com.tpa.entity.User;
import com.tpa.exception.GlobalExceptionHandler;
import com.tpa.repository.UserRepository;
import com.tpa.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;
    private Principal mockPrincipal;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("admin@tpa.com");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("admin@tpa.com");
    }

    @Test
    @DisplayName("TC-NOTIF-07: Get Notifications returns list")
    void getNotifications_success() throws Exception {
        when(userRepository.findByEmail("admin@tpa.com")).thenReturn(Optional.of(testUser));
        NotificationResponse res = NotificationResponse.builder().id(100L).title("Test").build();
        when(notificationService.getUserNotifications(1L)).thenReturn(List.of(res));

        mockMvc.perform(get("/api/v1/notifications").principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].title").value("Test"));
    }

    @Test
    @DisplayName("TC-NOTIF-08: Get Unread Count returns map")
    void getUnreadCount_success() throws Exception {
        when(userRepository.findByEmail("admin@tpa.com")).thenReturn(Optional.of(testUser));
        when(notificationService.countUnread(1L)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/notifications/unread-count").principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    @DisplayName("TC-NOTIF-09: Mark All As Read returns 200")
    void markAllAsRead_success() throws Exception {
        when(userRepository.findByEmail("admin@tpa.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/v1/notifications/mark-read").principal(mockPrincipal))
                .andExpect(status().isOk());

        verify(notificationService).markAllAsRead(1L);
    }

    @Test
    @DisplayName("TC-NOTIF-10: Mark One As Read returns 200")
    void markOneAsRead_success() throws Exception {
        when(userRepository.findByEmail("admin@tpa.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(patch("/api/v1/notifications/100/read").principal(mockPrincipal))
                .andExpect(status().isOk());

        verify(notificationService).markOneAsRead(100L, 1L);
    }
}
