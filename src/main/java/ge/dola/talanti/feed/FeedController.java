package ge.dola.talanti.feed;

import ge.dola.talanti.feed.dto.FeedResponseDto;
import ge.dola.talanti.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * React will call: GET /api/feed?limit=20
     * To get the next page: GET /api/feed?cursor=150&limit=20
     */
    @GetMapping
    public ResponseEntity<FeedResponseDto> getFeed(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {

        // Safety fallback (though our SecurityConfig should block unauthenticated requests before they get here)
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        // Pass the request to the service layer
        FeedResponseDto response = feedService.getFeed(currentUser.getId(), cursor, limit);

        return ResponseEntity.ok(response);
    }
}