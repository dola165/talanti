package ge.dola.talanti.notification;

import ge.dola.talanti.jooq.tables.records.NotificationsRecord;
import ge.dola.talanti.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationsRecord>> getMyNotifications() {
        return ResponseEntity.ok(notificationService.getMyNotifications(SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok().build();
    }
}