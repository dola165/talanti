package ge.dola.talanti.notification;

import ge.dola.talanti.notification.dto.NotificationBulkReadResultDto;
import ge.dola.talanti.notification.dto.NotificationDto;
import ge.dola.talanti.notification.dto.NotificationReadStateDto;
import ge.dola.talanti.notification.dto.NotificationUnreadCountDto;
import ge.dola.talanti.security.util.SecurityUtils;
import ge.dola.talanti.util.PageResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResult<NotificationDto>> getMyNotifications(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) @Positive Long clubId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(notificationService.getMyNotifications(SecurityUtils.getCurrentUserId(), scope, clubId, page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<NotificationUnreadCountDto> getUnreadCount(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) @Positive Long clubId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(SecurityUtils.getCurrentUserId(), scope, clubId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationReadStateDto> markAsRead(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id, SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<NotificationBulkReadResultDto> markAllAsRead(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) @Positive Long clubId) {
        return ResponseEntity.ok(notificationService.markAllAsRead(SecurityUtils.getCurrentUserId(), scope, clubId));
    }
}
