package ge.dola.talanti.tryout;

import ge.dola.talanti.security.CustomUserDetails;
import ge.dola.talanti.tryout.dto.TryoutApplicantDto;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/admin/tryouts")
public class TryoutAdminController {

    private final TryoutAdminService service;

    public TryoutAdminController(TryoutAdminService service) {
        this.service = service;
    }

    // NEW: Fetch by specific Club ID
    @GetMapping("/clubs/{clubId}/applications")
    public ResponseEntity<List<TryoutApplicantDto>> getApplicationsByClub(
            @PathVariable @Positive Long clubId,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(service.getApplicationsForClub(clubId));
    }

    // UPGRADED: Explicitly scopes the application update to the club domain
    @PutMapping("/clubs/{clubId}/applications/{applicationId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable @Positive Long clubId,
            @PathVariable @Positive Long applicationId,
            @RequestParam
            @Pattern(regexp = "(?i)PENDING|SHORTLISTED|ACCEPTED|REJECTED", message = "Status must be PENDING, SHORTLISTED, ACCEPTED, or REJECTED")
            String status) {
        service.updateApplicationStatus(clubId, applicationId, status);
        return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
    }
}
