package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.ClubProfileDto;
import ge.dola.talanti.club.dto.ClubRosterDto;
import ge.dola.talanti.club.dto.ClubStaffDto;
import ge.dola.talanti.club.dto.MyClubResponseDto;
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

    // 🟢 FIXED: Added the missing base endpoint for BrowseClubsPage
    @GetMapping
    public ResponseEntity<List<ClubProfileDto>> getAllClubs(@AuthenticationPrincipal CustomUserDetails currentUser) {
        Long currentUserId = (currentUser != null) ? currentUser.getId() : -1L;
        return ResponseEntity.ok(clubService.getAllClubs(currentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClubProfileDto> getClubProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        ClubProfileDto profile = clubService.getClubProfile(id, currentUser.getId());
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/my-club")
    public ResponseEntity<MyClubResponseDto> getMyClub(@AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        return clubService.getMyPrimaryClub(currentUser.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // --- 🟢 TABS ENDPOINTS ---

    @GetMapping("/{id}/feed")
    public ResponseEntity<Map<String, Object>> getClubFeed(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        FeedResponseDto response = feedService.getClubFeed(id, currentUser.getId(), null, 50);
        return ResponseEntity.ok(Map.of("posts", response.posts()));
    }

    @GetMapping("/{id}/roster")
    public ResponseEntity<List<ClubRosterDto>> getClubRoster(@PathVariable Long id) {
        return ResponseEntity.ok(clubService.getClubRoster(id));
    }

    @GetMapping("/{id}/staff")
    public ResponseEntity<List<ClubStaffDto>> getClubStaff(@PathVariable Long id) {
        return ResponseEntity.ok(clubService.getClubStaff(id));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<List<ge.dola.talanti.club.dto.CalendarEventDto>> getClubSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(clubService.getClubSchedule(id));
    }

    // --- EXISTING ENDPOINTS ---

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateClubImages(
            @PathVariable Long id,
            @RequestBody ge.dola.talanti.club.dto.ClubUpdateDto updateDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        clubService.updateClubImages(id, updateDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<Map<String, Boolean>> followClub(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        boolean isFollowing = clubService.toggleClubFollow(id, currentUser.getId());
        return ResponseEntity.ok(Map.of("following", isFollowing));
    }

    @PostMapping("/{id}/calendar")
    public ResponseEntity<?> createCalendarEvent(
            @PathVariable Long id,
            @RequestBody ge.dola.talanti.club.dto.CalendarRequestDto request,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        clubService.createCalendarEvent(id, user.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Event deployed successfully."));
    }

    @PutMapping("/{id}/calendar/{eventId}")
    public ResponseEntity<?> updateCalendarEvent(
            @PathVariable Long id,
            @PathVariable String eventId,
            @RequestBody ge.dola.talanti.club.dto.CalendarRequestDto request,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        try {
            clubService.updateCalendarEvent(id, eventId, request);
            return ResponseEntity.ok(Map.of("message", "Event updated successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/calendar/{eventId}")
    public ResponseEntity<?> deleteCalendarEvent(
            @PathVariable Long id,
            @PathVariable String eventId,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        try {
            clubService.deleteCalendarEvent(id, eventId);
            return ResponseEntity.ok(Map.of("message", "Event deleted successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}