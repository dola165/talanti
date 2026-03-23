package ge.dola.talanti.chat;

import ge.dola.talanti.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static ge.dola.talanti.jooq.Tables.*;

@Repository
@RequiredArgsConstructor
public class ChatRepository {

    private final DSLContext dsl;

    // 1. Save the message and return the enriched DTO
    public ChatMessageResponse saveMessage(Long conversationId, Long senderId, String content) {
        Long messageId = dsl.insertInto(MESSAGES)
                .set(MESSAGES.CONVERSATION_ID, conversationId)
                .set(MESSAGES.SENDER_ID, senderId)
                .set(MESSAGES.CONTENT, content)
                .set(MESSAGES.CREATED_AT, LocalDateTime.now())
                .returningResult(MESSAGES.ID)
                .fetchOneInto(Long.class);

        // Fetch it right back with the sender's name to broadcast to the room
        return dsl.select(
                        MESSAGES.ID,
                        MESSAGES.CONVERSATION_ID,
                        MESSAGES.SENDER_ID,
                        DSL.coalesce(USER_PROFILES.FULL_NAME, USERS.USERNAME).as("senderName"),
                        MESSAGES.CONTENT,
                        MESSAGES.CREATED_AT
                )
                .from(MESSAGES)
                .join(USERS).on(MESSAGES.SENDER_ID.eq(USERS.ID))
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .where(MESSAGES.ID.eq(messageId))
                .fetchOneInto(ChatMessageResponse.class);
    }

    // 2. Get all users in a conversation (so we know who to send the WebSocket to)
    public List<Long> getParticipantIds(Long conversationId) {
        return dsl.select(CONVERSATION_PARTICIPANTS.USER_ID)
                .from(CONVERSATION_PARTICIPANTS)
                .where(CONVERSATION_PARTICIPANTS.CONVERSATION_ID.eq(conversationId))
                .fetchInto(Long.class);
    }

    public boolean isConversationParticipant(Long conversationId, Long userId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(CONVERSATION_PARTICIPANTS)
                        .where(CONVERSATION_PARTICIPANTS.CONVERSATION_ID.eq(conversationId))
                        .and(CONVERSATION_PARTICIPANTS.USER_ID.eq(userId))
        );
    }
}
