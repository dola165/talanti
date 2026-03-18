package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @PostMapping
    public ResponseEntity<ClubDto> createClub(@Valid @RequestBody CreateClubDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clubService.createClub(dto));
    }

    @GetMapping
    public ResponseEntity<List<ClubProfileDto>> getAllClubs() {
        return ResponseEntity.ok(clubService.getAllClubs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClubProfileDto> getClubProfile(@PathVariable Long id) {
        return ResponseEntity.ok(clubService.getClubProfile(id));
    }

    @GetMapping("/my-club")
    public ResponseEntity<MyClubResponseDto> getMyClub() {
        return clubService.getMyPrimaryClub()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
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
    public ResponseEntity<List<CalendarEventDto>> getClubSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(clubService.getInternalClubSchedule(id));
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<Map<String, Boolean>> followClub(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("following", clubService.toggleClubFollow(id)));
    }

    @PostMapping("/{id}/challenge")
    public ResponseEntity<?> challengeClub(@PathVariable Long id, @RequestBody CreateChallengeDto dto) {
        clubService.createChallenge(id, dto);
        return ResponseEntity.ok(Map.of("message", "Challenge issued successfully."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateClubImages(@PathVariable Long id, @RequestBody ClubUpdateDto updateDto) {
        clubService.updateClubImages(id, updateDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/calendar")
    public ResponseEntity<?> createCalendarEvent(@PathVariable Long id, @RequestBody CalendarRequestDto request) {
        clubService.createCalendarEvent(id, request);
        return ResponseEntity.ok(Map.of("message", "Event deployed successfully."));
    }

    @DeleteMapping("/{id}/calendar/{eventId}")
    public ResponseEntity<?> deleteCalendarEvent(@PathVariable Long id, @PathVariable String eventId) {
        clubService.deleteCalendarEvent(id, eventId);
        return ResponseEntity.ok(Map.of("message", "Event deleted successfully."));
    }
}