package ge.dola.talanti.club.management;

import ge.dola.talanti.club.management.dto.CreateHonourDto;
import ge.dola.talanti.club.management.dto.CreateOpportunityDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubManagementController {

    private final ClubManagementService managementService;

    // --- OPPORTUNITIES (GoFundMe, Jobs, Volunteers) ---

    @PostMapping("/opportunities")
    public ResponseEntity<Map<String, Long>> addOpportunity(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateOpportunityDto dto) {
        Long id = managementService.addOpportunity(clubId, dto);
        return ResponseEntity.ok(Map.of("opportunityId", id));
    }

    @DeleteMapping("/opportunities/{opportunityId}")
    public ResponseEntity<Void> deleteOpportunity(
            @PathVariable Long clubId,
            @PathVariable Long opportunityId) {
        managementService.deleteOpportunity(clubId, opportunityId);
        return ResponseEntity.ok().build();
    }

    // --- HONOURS (Trophies & History) ---

    @PostMapping("/honours")
    public ResponseEntity<Map<String, Long>> addHonour(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateHonourDto dto) {
        Long id = managementService.addHonour(clubId, dto);
        return ResponseEntity.ok(Map.of("honourId", id));
    }

    @DeleteMapping("/honours/{honourId}")
    public ResponseEntity<Void> deleteHonour(
            @PathVariable Long clubId,
            @PathVariable Long honourId) {
        managementService.deleteHonour(clubId, honourId);
        return ResponseEntity.ok().build();
    }
}