package ge.dola.talanti.dto;

import java.time.LocalDateTime;

public class NotificationDTOs {

    public record NotificationResponse(
            Long id,
            String title,
            String body,
            String type,        // "FRIEND_REQUEST", "NEW_MESSAGE"
            boolean isRead,
            LocalDateTime createdAt
    ) {}

    public record MediaResponse(
            Long id,
            String url,
            String type,
            Long sizeBytes
    ) {}
}