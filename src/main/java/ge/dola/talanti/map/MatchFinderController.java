package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MatchRequestDto;
import ge.dola.talanti.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/matchfinder")
public class MatchFinderController {

    private final MatchFinderService matchFinderService;

    public MatchFinderController(MatchFinderService matchFinderService) {
        this.matchFinderService = matchFinderService;
    }

    @PostMapping
    public ResponseEntity<?> createRequest(
            @RequestBody MatchRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) return ResponseEntity.status(401).build();

        try {
            matchFinderService.createMatchRequest(currentUser.getId(), dto);
            return ResponseEntity.ok(Map.of("message", "Match request broadcasted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}