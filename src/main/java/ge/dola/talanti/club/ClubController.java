package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.ClubProfileDto;
import ge.dola.talanti.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}