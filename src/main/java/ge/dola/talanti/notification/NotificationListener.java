package ge.dola.talanti.notification;

import ge.dola.talanti.notification.event.NotificationEvent;
import ge.dola.talanti.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepository;

    // TEMPORARILY DISABLED FOR MVP (WebSockets pushed to Phase 3)
    // private final SimpMessagingTemplate messagingTemplate;

    @Async
    @EventListener
    public void handleNotification(NotificationEvent event) {

        if ("CLUB_ANNOUNCEMENT".equals(event.type()) && event.sourceClubId() != null) {
            notificationRepo.createClubAnnouncementNotifications(
                    event.sourceClubId(), event.entityId(), event.title(), event.body()
            );
        } else if (event.targetUserId() != null) {
            notificationRepo.saveSingle(
                    event.targetUserId(), event.type(), event.entityType(),
                    event.entityId(), event.title(), event.body()
            );

            // TODO: Uncomment when WebSockets are implemented
            /*
            userRepository.findById(event.targetUserId()).ifPresent(user -> {
                messagingTemplate.convertAndSendToUser(
                        user.getUsername(),
                        "/queue/notifications",
                        event
                );
            });
            */
        }
    }
}