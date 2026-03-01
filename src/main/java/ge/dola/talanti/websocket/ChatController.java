package ge.dola.talanti.websocket;

import ge.dola.talanti.dto.ChatDTOs;
import ge.dola.talanti.jooq.tables.records.UsersRecord;
import ge.dola.talanti.security.CustomUserDetails;
import org.jooq.DSLContext;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Import your generated tables (Ensure these are blue/recognized by IDE)
import static ge.dola.talanti.jooq.Tables.MESSAGES;
import static ge.dola.talanti.jooq.Tables.USERS;
import static ge.dola.talanti.jooq.Tables.CONVERSATIONS;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;
    private final DSLContext dsl;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          WebSocketSessionManager sessionManager,
                          DSLContext dsl) {
        this.messagingTemplate = messagingTemplate;
        this.sessionManager = sessionManager;
        this.dsl = dsl;
    }

    @MessageMapping("/message")
    @Transactional
    public void handleMessage(ChatDTOs.MessageInput input, org.springframework.security.core.Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();
        String username = userDetails.getUser().getUsername();

        // Log it to verify it works
        System.out.println("📩 Received: " + input.message() + " from " + username + " (ID: " + userId + ")");

        // 2. Save Message to DB
        var record = dsl.newRecord(MESSAGES);
        record.setSenderId(userId);
        record.setContent(input.message()); // Mapping 'message' from DTO to 'content' in DB
        record.setConversationId(input.conversationId() != null ? input.conversationId() : 1L);
        record.store();

        // 3. Update 'last_message_at' for the conversation
        dsl.update(CONVERSATIONS)
                .set(CONVERSATIONS.CREATED_AT, LocalDateTime.now())
                .where(CONVERSATIONS.ID.eq(input.conversationId() != null ? input.conversationId() : 1L))
                .execute();

        // 4. Send back to Clients
        // We broadcast a more complete response or just the message
        ChatDTOs.MessageInput broadcast = new ChatDTOs.MessageInput(username, input.message(), input.conversationId());
        messagingTemplate.convertAndSend("/topic/messages", broadcast);
    }

    @MessageMapping("/connect")
    public void connectUser(org.springframework.security.core.Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String username = userDetails.getUser().getUsername();
        sessionManager.addUsername(username);
        System.out.println("✅ Connected: " + username);
    }

    @MessageMapping("/disconnect")
    public void disconnectUser(org.springframework.security.core.Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String username = userDetails.getUser().getUsername();
        sessionManager.removeUsername(username);
        System.out.println("❌ Disconnected: " + username);
    }

    @MessageMapping("/request-users")
    public void requestUsers(){
        sessionManager.broadcastActiveUsernames();
    }
}