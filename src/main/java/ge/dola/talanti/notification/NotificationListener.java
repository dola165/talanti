package ge.dola.talanti.notification;

import ge.dola.talanti.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void handleNotification(NotificationEvent event) {

        if ("CLUB_ANNOUNCEMENT".equals(event.type()) && event.sourceClubId() != null) {
            notificationService.createClubAnnouncementNotifications(
                    event.sourceClubId(), event.entityId(), event.title(), event.body(), event.linkPath()
            );
        } else if (event.targetUserId() != null) {
            notificationService.createDirectNotification(
                    event.targetUserId(), event.type(), event.entityType(),
                    event.entityId(), event.title(), event.body(), event.linkPath()
            );
        }
    }
}
