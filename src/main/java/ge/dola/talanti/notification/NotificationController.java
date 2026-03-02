package ge.dola.talanti.notification;

import ge.dola.talanti.jooq.tables.records.NotificationsRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static ge.dola.talanti.jooq.Tables.NOTIFICATIONS;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final DSLContext dsl;

    @GetMapping
    public List<NotificationsRecord> getMyNotifications() {
        // Hardcoded for 'react_dev' for your current dev environment
        Long myUserId = dsl.select(ge.dola.talanti.jooq.Tables.USERS.ID)
                .from(ge.dola.talanti.jooq.Tables.USERS)
                .where(ge.dola.talanti.jooq.Tables.USERS.USERNAME.eq("react_dev"))
                .fetchOneInto(Long.class);

        return dsl.selectFrom(NOTIFICATIONS)
                .where(NOTIFICATIONS.USER_ID.eq(myUserId))
                .orderBy(NOTIFICATIONS.CREATED_AT.desc())
                .limit(20)
                .fetch();
    }

    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        dsl.update(NOTIFICATIONS)
                .set(NOTIFICATIONS.IS_READ, true)
                .where(NOTIFICATIONS.ID.eq(id))
                .execute();
    }
}