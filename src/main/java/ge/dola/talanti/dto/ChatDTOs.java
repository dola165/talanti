package ge.dola.talanti.dto;

import java.time.LocalDateTime;

public class ChatDTOs {

    /**
     * INCOMING: What the user types.
     * Matches your Swing Client JSON: { "user": "dola", "message": "hello" }
     */
    public record MessageInput(
            String user,       // Username (e.g. "dola_dev")
            String message,    // CHANGED from 'content' to 'message' to match Swing Client
            Long conversationId // Optional: Defaults to 1 if null
    ) {}

    /**
     * OUTGOING: What the server broadcasts back.
     */
    public record MessageResponse(
            Long id,
            Long conversationId,
            UserDTOs.UserSummary sender,
            String content,
            LocalDateTime sentAt
    ) {}
}