package ge.dola.talanti.notification;

import ge.dola.talanti.club.ClubRole;
import ge.dola.talanti.notification.dto.NotificationDto;
import ge.dola.talanti.util.PageResult;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.CLUBS;
import static ge.dola.talanti.jooq.Tables.CLUB_MEMBERSHIPS;
import static ge.dola.talanti.jooq.Tables.NOTIFICATIONS;
import static ge.dola.talanti.jooq.tables.ClubFollows.CLUB_FOLLOWS;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {

    private static final List<String> CLUB_SCOPE_ACCESS_ROLES = List.of(
            ClubRole.OWNER.name(),
            ClubRole.CLUB_ADMIN.name(),
            ClubRole.COACH.name()
    );

    private final DSLContext dsl;

    public void createNotifications(Collection<Long> userIds,
                                    String type,
                                    String entityType,
                                    Long entityId,
                                    String title,
                                    String body,
                                    NotificationScope scope,
                                    Long clubId,
                                    String linkPath) {
        List<Long> distinctUserIds = userIds == null ? List.of() : userIds.stream()
                .filter(userId -> userId != null)
                .distinct()
                .toList();

        if (distinctUserIds.isEmpty()) {
            return;
        }

        dsl.batch(
                distinctUserIds.stream()
                        .map(userId -> dsl.insertInto(NOTIFICATIONS)
                                .set(NOTIFICATIONS.USER_ID, userId)
                                .set(NOTIFICATIONS.TYPE, type)
                                .set(NOTIFICATIONS.ENTITY_TYPE, entityType)
                                .set(NOTIFICATIONS.ENTITY_ID, entityId)
                                .set(NOTIFICATIONS.TITLE, title)
                                .set(NOTIFICATIONS.BODY, body)
                                .set(NOTIFICATIONS.IS_READ, false)
                                .set(NOTIFICATIONS.CREATED_AT, LocalDateTime.now())
                                .set(NotificationDynamicFields.NOTIFICATIONS_SCOPE, scope.name())
                                .set(NotificationDynamicFields.NOTIFICATIONS_CLUB_ID, clubId)
                                .set(NotificationDynamicFields.NOTIFICATIONS_LINK_PATH, linkPath))
                        .toArray(org.jooq.Query[]::new)
        ).execute();
    }

    public void createClubAnnouncementNotifications(Long clubId, Long postId, String title, String body, String linkPath) {
        dsl.insertInto(NOTIFICATIONS,
                        NOTIFICATIONS.USER_ID, NOTIFICATIONS.TYPE, NOTIFICATIONS.ENTITY_TYPE,
                        NOTIFICATIONS.ENTITY_ID, NOTIFICATIONS.TITLE, NOTIFICATIONS.BODY,
                        NOTIFICATIONS.IS_READ, NOTIFICATIONS.CREATED_AT,
                        NotificationDynamicFields.NOTIFICATIONS_SCOPE,
                        NotificationDynamicFields.NOTIFICATIONS_CLUB_ID,
                        NotificationDynamicFields.NOTIFICATIONS_LINK_PATH)
                .select(
                        dsl.select(
                                        CLUB_FOLLOWS.USER_ID,
                                        DSL.inline("CLUB_ANNOUNCEMENT"),
                                        DSL.inline("post"),
                                        DSL.val(postId),
                                        DSL.val(title),
                                        DSL.val(body),
                                        DSL.inline(false),
                                        DSL.currentLocalDateTime(),
                                        DSL.inline(NotificationScope.PERSONAL.name()),
                                        DSL.val(clubId),
                                        DSL.val(linkPath)
                                )
                                .from(CLUB_FOLLOWS)
                                .where(CLUB_FOLLOWS.CLUB_ID.eq(clubId))
                )
                .execute();
    }

    public PageResult<NotificationDto> getUserNotifications(Long userId,
                                                            NotificationQueryScope scope,
                                                            Long clubId,
                                                            int page,
                                                            int size) {
        Condition visibilityCondition = accessibleNotificationsCondition(userId);
        Condition filterCondition = scopeCondition(scope, clubId);
        Condition combinedCondition = visibilityCondition.and(filterCondition);

        long total = dsl.selectCount()
                .from(NOTIFICATIONS)
                .where(combinedCondition)
                .fetchOne(0, Long.class);

        List<NotificationDto> content = dsl.select(notificationSelect())
                .from(NOTIFICATIONS)
                .leftJoin(CLUBS).on(CLUBS.ID.eq(NotificationDynamicFields.NOTIFICATIONS_CLUB_ID))
                .where(combinedCondition)
                .orderBy(NOTIFICATIONS.CREATED_AT.desc(), NOTIFICATIONS.ID.desc())
                .limit(size)
                .offset(page * size)
                .fetch(this::mapNotification);

        return new PageResult<>(content, page, size, total);
    }

    public long countUnread(Long userId, NotificationQueryScope scope, Long clubId) {
        return dsl.selectCount()
                .from(NOTIFICATIONS)
                .where(accessibleNotificationsCondition(userId))
                .and(scopeCondition(scope, clubId))
                .and(NOTIFICATIONS.IS_READ.eq(false))
                .fetchOne(0, Long.class);
    }

    public Optional<NotificationDto> findAccessibleNotification(Long id, Long userId) {
        return dsl.select(notificationSelect())
                .from(NOTIFICATIONS)
                .leftJoin(CLUBS).on(CLUBS.ID.eq(NotificationDynamicFields.NOTIFICATIONS_CLUB_ID))
                .where(NOTIFICATIONS.ID.eq(id))
                .and(accessibleNotificationsCondition(userId))
                .fetchOptional(this::mapNotification);
    }

    public void markAsRead(Long id, Long userId) {
        dsl.update(NOTIFICATIONS)
                .set(NOTIFICATIONS.IS_READ, true)
                .where(NOTIFICATIONS.ID.eq(id)
                        .and(NOTIFICATIONS.USER_ID.eq(userId)))
                .execute();
    }

    public long markAllAsRead(Long userId, NotificationQueryScope scope, Long clubId) {
        return dsl.update(NOTIFICATIONS)
                .set(NOTIFICATIONS.IS_READ, true)
                .where(accessibleNotificationsCondition(userId))
                .and(scopeCondition(scope, clubId))
                .and(NOTIFICATIONS.IS_READ.eq(false))
                .execute();
    }

    private Field<?>[] notificationSelect() {
        return new Field[]{
                NOTIFICATIONS.ID,
                NOTIFICATIONS.TYPE,
                NotificationDynamicFields.NOTIFICATIONS_SCOPE.as("scope"),
                NotificationDynamicFields.NOTIFICATIONS_CLUB_ID.as("clubId"),
                CLUBS.NAME.as("clubName"),
                NOTIFICATIONS.ENTITY_TYPE,
                NOTIFICATIONS.ENTITY_ID,
                NOTIFICATIONS.TITLE,
                NOTIFICATIONS.BODY,
                NOTIFICATIONS.IS_READ,
                NOTIFICATIONS.CREATED_AT,
                NotificationDynamicFields.NOTIFICATIONS_LINK_PATH.as("linkPath")
        };
    }

    private NotificationDto mapNotification(Record record) {
        return new NotificationDto(
                record.get(NOTIFICATIONS.ID),
                record.get(NOTIFICATIONS.TYPE),
                record.get("scope", String.class),
                record.get("clubId", Long.class),
                record.get("clubName", String.class),
                record.get(NOTIFICATIONS.ENTITY_TYPE),
                record.get(NOTIFICATIONS.ENTITY_ID),
                record.get(NOTIFICATIONS.TITLE),
                record.get(NOTIFICATIONS.BODY),
                Boolean.TRUE.equals(record.get(NOTIFICATIONS.IS_READ)),
                record.get(NOTIFICATIONS.CREATED_AT),
                record.get("linkPath", String.class)
        );
    }

    private Condition accessibleNotificationsCondition(Long userId) {
        Condition ownedByUser = NOTIFICATIONS.USER_ID.eq(userId);
        Condition personalVisibility = NotificationDynamicFields.NOTIFICATIONS_SCOPE.eq(NotificationScope.PERSONAL.name())
                .or(NotificationDynamicFields.NOTIFICATIONS_SCOPE.isNull());
        Condition clubVisibility = NotificationDynamicFields.NOTIFICATIONS_SCOPE.eq(NotificationScope.CLUB.name())
                .and(NotificationDynamicFields.NOTIFICATIONS_CLUB_ID.isNotNull())
                .and(DSL.exists(
                        dsl.selectOne()
                                .from(CLUB_MEMBERSHIPS)
                                .where(CLUB_MEMBERSHIPS.USER_ID.eq(userId))
                                .and(CLUB_MEMBERSHIPS.CLUB_ID.eq(NotificationDynamicFields.NOTIFICATIONS_CLUB_ID))
                                .and(CLUB_MEMBERSHIPS.ROLE.in(CLUB_SCOPE_ACCESS_ROLES))
                ));

        return ownedByUser.and(personalVisibility.or(clubVisibility));
    }

    private Condition scopeCondition(NotificationQueryScope scope, Long clubId) {
        if (scope == null || scope == NotificationQueryScope.ALL) {
            return DSL.trueCondition();
        }

        Condition condition = NotificationDynamicFields.NOTIFICATIONS_SCOPE.eq(scope.name());
        if (clubId != null) {
            condition = condition.and(NotificationDynamicFields.NOTIFICATIONS_CLUB_ID.eq(clubId));
        }
        return condition;
    }
}
