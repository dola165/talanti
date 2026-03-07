package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.ClubProfileDto;
import ge.dola.talanti.feed.FeedService;
import ge.dola.talanti.feed.dto.FeedResponseDto;
import ge.dola.talanti.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;
    private final FeedService feedService;

    /**
     * React will call: GET /api/clubs/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClubProfileDto> getClubProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        ClubProfileDto profile = clubService.getClubProfile(id, currentUser.getId());
        return ResponseEntity.ok(profile);
    }

    /**
     * React will call: POST /api/clubs/1/follow
     */
    @PostMapping("/{id}/follow")
    public ResponseEntity<?> toggleFollow(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        try {
            boolean isNowFollowing = clubService.toggleClubFollow(id, currentUser.getId());

            // Return a simple map that Spring converts to JSON: { "isFollowed": true }
            return ResponseEntity.ok(Map.of("isFollowed", isNowFollowing));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<ClubProfileDto>> getAllClubs(@AuthenticationPrincipal CustomUserDetails currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : 1L; // Fallback to user 1 for MVP testing
        return ResponseEntity.ok(clubService.getAllClubs(userId));
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<FeedResponseDto> getClubFeed(
            @PathVariable Long clubId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {

        Long userId = currentUser != null ? currentUser.getId() : 1L;
        return ResponseEntity.ok(feedService.getClubFeed(clubId, userId, cursor, limit));
    }

    @GetMapping("/user/{authorId}")
    public ResponseEntity<FeedResponseDto> getUserFeed(
            @PathVariable Long authorId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {

        Long userId = currentUser != null ? currentUser.getId() : 1L;
        return ResponseEntity.ok(feedService.getUserFeed(authorId, userId, cursor, limit));
    }
}