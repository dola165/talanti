package ge.dola.talanti.club.management;

import ge.dola.talanti.club.management.dto.CreateHonourDto;
import ge.dola.talanti.club.management.dto.ClubManagementOverviewDto;
import ge.dola.talanti.club.management.dto.CreateClubInviteDto;
import ge.dola.talanti.club.management.dto.CreateOpportunityDto;
import ge.dola.talanti.club.management.dto.UpdateClubMemberRoleDto;
import ge.dola.talanti.user.dto.UserSearchDto;
import ge.dola.talanti.util.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubManagementController {

    private final ClubManagementService managementService;

    @GetMapping("/management")
    public ResponseEntity<ClubManagementOverviewDto> getManagementOverview(
            @PathVariable @Positive Long clubId) {
        return ResponseEntity.ok(managementService.getManagementOverview(clubId));
    }

    @GetMapping("/management/user-search")
    public ResponseEntity<PageResult<UserSearchDto>> searchUsersForInvite(
            @PathVariable @Positive Long clubId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int size) {
        return ResponseEntity.ok(managementService.searchUsersForInvite(clubId, query, page, size));
    }

    @PostMapping("/management/invitations")
    public ResponseEntity<Map<String, Long>> createInvitation(
            @PathVariable @Positive Long clubId,
            @Valid @RequestBody CreateClubInviteDto dto) {
        Long inviteId = managementService.createInvitation(clubId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("invitationId", inviteId));
    }

    @PatchMapping("/management/members/{userId}/role")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable @Positive Long clubId,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdateClubMemberRoleDto dto) {
        managementService.updateMemberRole(clubId, userId, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/management/invitations/{inviteId}")
    public ResponseEntity<Void> cancelInvitation(
            @PathVariable @Positive Long clubId,
            @PathVariable @Positive Long inviteId) {
        managementService.cancelInvitation(clubId, inviteId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/opportunities")
    public ResponseEntity<Map<String, Long>> addOpportunity(
            @PathVariable @Positive Long clubId,
            @Valid @RequestBody CreateOpportunityDto dto) {
        Long id = managementService.addOpportunity(clubId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("opportunityId", id));
    }

    @DeleteMapping("/opportunities/{opportunityId}")
    public ResponseEntity<Void> deleteOpportunity(
            @PathVariable @Positive Long clubId,
            @PathVariable @Positive Long opportunityId) {
        managementService.deleteOpportunity(clubId, opportunityId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/honours")
    public ResponseEntity<Map<String, Long>> addHonour(
            @PathVariable @Positive Long clubId,
            @Valid @RequestBody CreateHonourDto dto) {
        Long id = managementService.addHonour(clubId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("honourId", id));
    }

    @DeleteMapping("/honours/{honourId}")
    public ResponseEntity<Void> deleteHonour(
            @PathVariable @Positive Long clubId,
            @PathVariable @Positive Long honourId) {
        managementService.deleteHonour(clubId, honourId);
        return ResponseEntity.ok().build();
    }
}
