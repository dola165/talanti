package ge.dola.talanti.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ConversationDTOs {

    public record ConversationSummary(
            Long id,
            String type,        // 'G' or 'P'
            String name,        // "General Chat" or "Dola & Gio"
            LocalDateTime lastMessageAt
    ) {}

    public record CreateGroupRequest(
            String name,
            List<String> participantUsernames
    ) {}
}