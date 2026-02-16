package ge.dola.talanti.websocket;

import ge.dola.talanti.dto.ChatDTOs;
import ge.dola.talanti.jooq.tables.records.UsersRecord;
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
    public void handleMessage(ChatDTOs.MessageInput input) {
        // Log it to verify it works
        System.out.println("📩 Received: " + input.message() + " from " + input.user());

        // 1. Find or Create User (to get ID)
        Long userId = getOrCreateUserId(input.user());

        // 2. Save Message to DB
        var record = dsl.newRecord(MESSAGES);
        record.setSenderId(userId);
        record.setContent(input.message()); // Mapping 'message' from DTO to 'content' in DB
        record.setConversationId(1L); // Hardcoded to General Chat
        record.store();

        // 3. Update 'last_message_at' for the conversation
        dsl.update(CONVERSATIONS)
                .set(CONVERSATIONS.CREATED_AT, LocalDateTime.now())
                .where(CONVERSATIONS.ID.eq(1L))
                .execute();

        // 4. Send back to Clients
        // We send the input back because it matches exactly what the Swing Client expects:
        // { "user": "...", "message": "..." }
        messagingTemplate.convertAndSend("/topic/messages", input);
    }

    @MessageMapping("/connect")
    public void connectUser(String username) {
        sessionManager.addUsername(username);
        getOrCreateUserId(username); // Ensure they exist in DB
        System.out.println("✅ Connected: " + username);
    }

    @MessageMapping("/disconnect")
    public void disconnectUser(String username) {
        sessionManager.removeUsername(username);
        System.out.println("❌ Disconnected: " + username);
    }

    @MessageMapping("/request-users")
    public void requestUsers(){
        sessionManager.broadcastActiveUsernames();
    }

    // Helper: Finds user ID by name, or creates them if missing
    private Long getOrCreateUserId(String username) {
        var existingUser = dsl.selectFrom(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOne();

        if (existingUser != null) {
            return existingUser.getId();
        } else {
            var newUser = dsl.newRecord(USERS);
            newUser.setUsername(username);
            newUser.setPasswordHash("placeholder_hash");
            newUser.store();
            return newUser.getId();
        }
    }
}