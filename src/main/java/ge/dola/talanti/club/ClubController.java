package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.ClubProfileDto;
import ge.dola.talanti.club.dto.ClubRosterDto;
import ge.dola.talanti.club.dto.ClubStaffDto;
import ge.dola.talanti.club.dto.MyClubResponseDto;
import ge.dola.talanti.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

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
    @GetMapping("/my-club")
    public ResponseEntity<?> getMyClub(@AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();

        Optional<MyClubResponseDto> myClub = clubService.getMyPrimaryClub(currentUser.getId());

        if (myClub.isPresent()) {
            return ResponseEntity.ok(myClub.get());
        } else {
            // Return 204 No Content (Standard practice for "You don't have one")
            return ResponseEntity.noContent().build();
        }
    }
    @GetMapping
    public ResponseEntity<List<ClubProfileDto>> getAllClubs(@AuthenticationPrincipal CustomUserDetails currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : 1L; // Fallback to user 1 for MVP testing
        return ResponseEntity.ok(clubService.getAllClubs(userId));
    }

    @GetMapping("/{id}/roster")
    public ResponseEntity<List<ClubRosterDto>> getClubRoster(@PathVariable Long id) {
        return ResponseEntity.ok(clubService.getClubRoster(id));
    }

    @GetMapping("/{id}/staff")
    public ResponseEntity<List<ClubStaffDto>> getClubStaff(@PathVariable Long id) {
        return ResponseEntity.ok(clubService.getClubStaff(id));
    }

    @GetMapping("/{id}/calendar")
    public ResponseEntity<List<ge.dola.talanti.club.dto.CalendarEventDto>> getClubSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(clubService.getClubSchedule(id));
    }

    @PostMapping("/{id}/calendar")
    public ResponseEntity<?> addCalendarEvent(
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