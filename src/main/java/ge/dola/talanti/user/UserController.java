package ge.dola.talanti.user;

import ge.dola.talanti.security.CustomUserDetails;
import ge.dola.talanti.user.dto.PublicUserProfileDto;
import ge.dola.talanti.user.dto.UserSearchDto;
import ge.dola.talanti.user.repository.UserRepository;
import org.jooq.DSLContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static ge.dola.talanti.jooq.Tables.USERS;
import static ge.dola.talanti.jooq.Tables.USER_PROFILES;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final DSLContext dsl;
    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(DSLContext dsl, UserService userService, UserRepository userRepository) {
        this.dsl = dsl;
        this.userService = userService;
        this.userRepository = userRepository;
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

    @GetMapping("/{id}")
    public ResponseEntity<PublicUserProfileDto> getPublicProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(userService.getProfile(id, currentUser.getId()));
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<?> toggleFollow(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) return ResponseEntity.status(401).build();

        try {
            boolean isFollowed = userService.toggleFollow(id, currentUser.getId());
            return ResponseEntity.ok(Map.of("isFollowed", isFollowed));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchDto>> searchUsers(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(userRepository.searchUsers(query.trim()));
    }
}
