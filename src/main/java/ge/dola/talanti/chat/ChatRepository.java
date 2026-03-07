package ge.dola.talanti.chat;

import ge.dola.talanti.dto.ChatDTOs;
import ge.dola.talanti.dto.UserDTOs;
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

    public ChatDTOs.ChatMessageResponse saveMessage(Long conversationId, Long senderId, String content) {
        return dsl.transactionResult(config -> {
            DSLContext tx = DSL.using(config);

            long messageId = tx.insertInto(MESSAGES)
                    .set(MESSAGES.CONVERSATION_ID, conversationId)
                    .set(MESSAGES.SENDER_ID, senderId)
                    .set(MESSAGES.CONTENT, content)
                    .set(MESSAGES.CREATED_AT, LocalDateTime.now())
                    .returningResult(MESSAGES.ID)
                    .fetchOneInto(Long.class);

            return tx.select(
                            MESSAGES.ID,
                            MESSAGES.CONVERSATION_ID,
                            DSL.row(
                                    USERS.ID,
                                    USERS.USERNAME,
                                    USER_PROFILES.FULL_NAME
                            ).mapping(UserDTOs.UserSummary::new).as("sender"),
                            MESSAGES.CONTENT,
                            MESSAGES.CREATED_AT.as("sentAt")
                    )
                    .from(MESSAGES)
                    .join(USERS).on(MESSAGES.SENDER_ID.eq(USERS.ID))
                    .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                    .where(MESSAGES.ID.eq(messageId))
                    .fetchOneInto(ChatDTOs.ChatMessageResponse.class);
        });
    }

    public List<Long> getParticipantIds(Long conversationId) {
        return dsl.select(CONVERSATION_PARTICIPANTS.USER_ID)
                .from(CONVERSATION_PARTICIPANTS)
                .where(CONVERSATION_PARTICIPANTS.CONVERSATION_ID.eq(conversationId))
                .fetchInto(Long.class);
    }
}
