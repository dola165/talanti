package ge.dola.talanti.dto;

import java.time.LocalDateTime;

public class ChatDTOs {

    public record ChatMessageRequest(
            Long conversationId,
            String content
    ) {}

    public record ChatMessageResponse(
            Long id,
            Long conversationId,
            UserDTOs.UserSummary sender,
            String content,
            LocalDateTime sentAt
    ) {}
}
