package com.plover.backerymanagmentsystem.notification.service;

import com.plover.backerymanagmentsystem.notification.dto.NotificationDto;

import java.util.List;

public interface NotificationService {
    void sendNotification(String title, String message, String type, String targetRole);
    List<NotificationDto> getNotificationsForRole(String roleId);
    void markAsRead(Long notificationId);
    void markAllAsRead(String roleId);
}
