package ge.dola.talanti.tryout;

import ge.dola.talanti.security.CustomUserDetails;
import ge.dola.talanti.security.util.SecurityUtils;
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

    // UPGRADED: Explicitly scopes the application update to the club domain
    @PutMapping("/clubs/{clubId}/applications/{applicationId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long clubId,
            @PathVariable Long applicationId,
            @RequestParam String status) {

        try {
            // Passes the clubId to satisfy the @PreAuthorize proxy,
            // the applicationId to target the row,
            // and the adminId for the audit log.
            service.updateApplicationStatus(clubId, applicationId, status, SecurityUtils.getCurrentUserId());
            return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}