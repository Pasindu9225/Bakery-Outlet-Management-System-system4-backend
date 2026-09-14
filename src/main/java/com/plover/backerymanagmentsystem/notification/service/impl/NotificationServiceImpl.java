package com.plover.backerymanagmentsystem.notification.service.impl;

import com.plover.backerymanagmentsystem.notification.dto.NotificationDto;
import com.plover.backerymanagmentsystem.notification.model.Notification;
import com.plover.backerymanagmentsystem.notification.repository.NotificationRepository;
import com.plover.backerymanagmentsystem.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public void sendNotification(String title, String message, String type, String targetRole) {
        Notification notification = Notification.builder()
                .title(title)
                .message(message)
                .type(type)
                .targetRole(targetRole)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    public List<NotificationDto> getNotificationsForRole(String roleId) {
        return notificationRepository.findByTargetRoleOrderByCreatedAtDesc(roleId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Override
    public void markAllAsRead(String roleId) {
        List<Notification> unread = notificationRepository.findByTargetRoleAndReadFalseOrderByCreatedAtDesc(roleId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    private NotificationDto mapToDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .targetRole(n.getTargetRole())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .timeAgo(calculateTimeAgo(n.getCreatedAt()))
                .build();
    }

    private String calculateTimeAgo(LocalDateTime createdAt) {
        if (createdAt == null) return "Just now";
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) return "Just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }
}
