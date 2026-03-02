package ge.dola.talanti.notification;

import ge.dola.talanti.notification.event.NotificationEvent;
import ge.dola.talanti.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepository; // To get usernames for WebSockets
    private final SimpMessagingTemplate messagingTemplate;

    @Async // Runs on a background thread!
    @EventListener
    public void handleNotification(NotificationEvent event) {

        if ("CLUB_ANNOUNCEMENT".equals(event.type()) && event.sourceClubId() != null) {
            // 1. Blast to the database in one fast query
            notificationRepo.createClubAnnouncementNotifications(
                    event.sourceClubId(), event.entityId(), event.title(), event.body()
            );

            // Note: For club fan-outs, broadcasting live WebSockets to 10,000 users
            // individually is heavy. Usually, users just see this when they refresh or poll.
            // Let's stick to DB-only for fan-outs for now to keep the server from crashing.

        } else if (event.targetUserId() != null) {
            // 1. Save to database
            notificationRepo.saveSingle(
                    event.targetUserId(), event.type(), event.entityType(),
                    event.entityId(), event.title(), event.body()
            );

            // 2. Push Live via WebSocket
            userRepository.findById(event.targetUserId()).ifPresent(user -> {
                // The frontend will listen to: /user/queue/notifications
                messagingTemplate.convertAndSendToUser(
                        user.getUsername(),
                        "/queue/notifications",
                        event
                );
            });
        }
    }
}