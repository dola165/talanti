package ge.dola.talanti.notification;

import ge.dola.talanti.jooq.tables.records.NotificationsRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<NotificationsRecord> getMyNotifications(Long userId) {
        return notificationRepo.getUserNotifications(userId);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void markAsRead(Long notificationId, Long userId) {
        // RLS prevents users from marking other peoples' notifications as read natively
        notificationRepo.markAsRead(notificationId);
    }
}