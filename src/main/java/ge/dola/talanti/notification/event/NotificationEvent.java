package ge.dola.talanti.notification.event;

public record NotificationEvent(
        Long targetUserId, // Used for direct mentions/messages
        String type,       // 'POST_TAG', 'CLUB_ANNOUNCEMENT', 'NEW_MESSAGE'
        String entityType, // 'post', 'club', 'message'
        Long entityId,     // The ID to link to
        String title,
        String body,
        Long sourceClubId, // Used ONLY for fan-out
        String linkPath
) {}
