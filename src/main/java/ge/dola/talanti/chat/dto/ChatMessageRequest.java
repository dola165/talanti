package ge.dola.talanti.chat.dto;

public record ChatMessageRequest(
        Long conversationId,
        String content
) {}