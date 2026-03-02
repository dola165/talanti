package ge.dola.talanti.chat;

import lombok.Data;

@Data
public class ChatMessagePayload {
    private String content;
    private Long conversationId;
    private String recipientUsername; // Used for private 1-on-1 messages
}