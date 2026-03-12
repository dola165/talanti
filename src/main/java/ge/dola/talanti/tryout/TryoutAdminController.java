package ge.dola.talanti.tryout;

import ge.dola.talanti.security.CustomUserDetails;
import ge.dola.talanti.tryout.dto.TryoutApplicantDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/tryouts")
public class TryoutAdminController {

    private final TryoutAdminService service;

    public TryoutAdminController(TryoutAdminService service) {
        this.service = service;
    }

    // NEW: Fetch by specific Club ID
    @GetMapping("/clubs/{clubId}/applications")
    public ResponseEntity<List<TryoutApplicantDto>> getApplicationsByClub(
            @PathVariable Long clubId,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(service.getApplicationsForClub(clubId, user.getId()));
    }

    // EXISTING: Update Status
    @PutMapping("/applications/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();

        try {
            service.updateApplicationStatus(id, status, user.getId());
            return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}