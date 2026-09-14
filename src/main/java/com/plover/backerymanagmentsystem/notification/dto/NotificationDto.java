package com.plover.backerymanagmentsystem.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String title;
    private String message;
    private String type;
    private String targetRole;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String timeAgo; // Optional helper for frontend
}
