package ge.dola.talanti.user;

import ge.dola.talanti.security.CustomUserDetails;
import org.jooq.DSLContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static ge.dola.talanti.jooq.Tables.USERS;
import static ge.dola.talanti.jooq.Tables.USER_PROFILES;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final DSLContext dsl;

    public UserController(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Gets the profile of the currently logged-in user.
     * @AuthenticationPrincipal injects the details from our CookieAuthFilter.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        // Fetch User and Profile joined together using JOOQ
        var record = dsl.select(
                        USERS.ID,
                        USERS.USERNAME,
                        USERS.EMAIL,
                        USER_PROFILES.FULL_NAME,
                        USER_PROFILES.POSITION,
                        USER_PROFILES.BIO
                )
                .from(USERS)
                .leftJoin(USER_PROFILES).on(USERS.ID.eq(USER_PROFILES.USER_ID))
                .where(USERS.ID.eq(currentUser.getId()))
                .fetchOne();

        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        // For speed, returning an anonymous map/DTO. Better to use a MapStruct DTO here later.
        return ResponseEntity.ok(new UserProfileDto(
                record.get(USERS.ID),
                record.get(USERS.USERNAME),
                record.get(USERS.EMAIL),
                record.get(USER_PROFILES.FULL_NAME),
                record.get(USER_PROFILES.POSITION),
                record.get(USER_PROFILES.BIO)
        ));
    }
}
