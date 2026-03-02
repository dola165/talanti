package ge.dola.talanti.notification;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static ge.dola.talanti.jooq.Tables.NOTIFICATIONS;
import static ge.dola.talanti.jooq.tables.ClubFollows.CLUB_FOLLOWS;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {

    private final DSLContext dsl;

    // 1. Save a single notification (Mentions, Messages)
    public void saveSingle(Long userId, String type, String entityType, Long entityId, String title, String body) {
        dsl.insertInto(NOTIFICATIONS)
                .set(NOTIFICATIONS.USER_ID, userId)
                .set(NOTIFICATIONS.TYPE, type)
                .set(NOTIFICATIONS.ENTITY_TYPE, entityType)
                .set(NOTIFICATIONS.ENTITY_ID, entityId)
                .set(NOTIFICATIONS.TITLE, title)
                .set(NOTIFICATIONS.BODY, body)
                .set(NOTIFICATIONS.IS_READ, false)
                .set(NOTIFICATIONS.CREATED_AT, LocalDateTime.now())
                .execute();
    }

    // 2. Bulk insert for Club Announcements (Extremely fast database-level insert)
    public void createClubAnnouncementNotifications(Long clubId, Long postId, String title, String body) {
        dsl.insertInto(NOTIFICATIONS,
                        NOTIFICATIONS.USER_ID, NOTIFICATIONS.TYPE, NOTIFICATIONS.ENTITY_TYPE,
                        NOTIFICATIONS.ENTITY_ID, NOTIFICATIONS.TITLE, NOTIFICATIONS.BODY,
                        NOTIFICATIONS.IS_READ, NOTIFICATIONS.CREATED_AT)
                .select(
                        dsl.select(
                                        CLUB_FOLLOWS.USER_ID,
                                        DSL.inline("CLUB_ANNOUNCEMENT"),
                                        DSL.inline("post"),
                                        DSL.val(postId),
                                        DSL.val(title),
                                        DSL.val(body),
                                        DSL.inline(false),
                                        DSL.currentLocalDateTime()
                                )
                                .from(CLUB_FOLLOWS)
                                .where(CLUB_FOLLOWS.CLUB_ID.eq(clubId))
                )
                .execute();
    }
}