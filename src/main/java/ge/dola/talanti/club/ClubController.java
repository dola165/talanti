package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.*;
import ge.dola.talanti.schedule.dto.ScheduleItemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Validated
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
    public ResponseEntity<ClubProfileDto> getClubProfile(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(clubService.getClubProfile(id));
    }

    @GetMapping("/my-club")
    public ResponseEntity<MyClubResponseDto> getMyClub() {
        return clubService.getMyPrimaryClub()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/my-membership-context")
    public ResponseEntity<ClubMembershipContextDto> getMyMembershipContext() {
        return ResponseEntity.ok(clubService.getMyMembershipContext());
    }

    @GetMapping("/{id}/roster")
    public ResponseEntity<List<ClubRosterDto>> getClubRoster(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(clubService.getClubRoster(id));
    }

    @GetMapping("/{id}/staff")
    public ResponseEntity<List<ClubStaffDto>> getClubStaff(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(clubService.getClubStaff(id));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<List<CalendarEventDto>> getClubSchedule(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(clubService.getInternalClubSchedule(id));
    }

    @GetMapping("/{id}/calendar")
    public ResponseEntity<List<ScheduleItemDto>> getClubCalendar(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(clubService.getClubCalendar(id));
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<Map<String, Boolean>> followClub(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(Map.of("following", clubService.toggleClubFollow(id)));
    }

    @PostMapping("/{id}/challenge")
    public ResponseEntity<?> challengeClub(@PathVariable @Positive Long id, @Valid @RequestBody CreateChallengeDto dto) {
        clubService.createChallenge(id, dto);
        return ResponseEntity.ok(Map.of("message", "Challenge issued successfully."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateClubImages(@PathVariable @Positive Long id, @Valid @RequestBody ClubUpdateDto updateDto) {
        clubService.updateClubImages(id, updateDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/calendar")
    public ResponseEntity<?> createCalendarEvent(@PathVariable @Positive Long id, @Valid @RequestBody CalendarRequestDto request) {
        clubService.createCalendarEvent(id, request);
        return ResponseEntity.ok(Map.of("message", "Event deployed successfully."));
    }

    @DeleteMapping("/{id}/calendar/{eventId}")
    public ResponseEntity<?> deleteCalendarEvent(@PathVariable @Positive Long id, @PathVariable String eventId) {
        clubService.deleteCalendarEvent(id, eventId);
        return ResponseEntity.ok(Map.of("message", "Event deleted successfully."));
    }
}
