package ge.dola.talanti.chat.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime createdAt
) {}