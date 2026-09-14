package com.plover.backerymanagmentsystem.notification.controller;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.notification.dto.NotificationDto;
import com.plover.backerymanagmentsystem.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getMyNotifications(@AuthenticationPrincipal AuthModel user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(notificationService.getNotificationsForRole(user.getRoleId()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AuthModel user) {
        if (user != null) {
            notificationService.markAllAsRead(user.getRoleId());
        }
        return ResponseEntity.ok().build();
    }
}
