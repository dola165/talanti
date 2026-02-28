package ge.dola.talanti.feed;

import ge.dola.talanti.feed.dto.CommentDto;
import ge.dola.talanti.feed.dto.CommentRequest;
import ge.dola.talanti.feed.dto.FeedResponseDto;
import ge.dola.talanti.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/club/{clubId}")
    public ResponseEntity<FeedResponseDto> getClubFeed(
            @PathVariable Long clubId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {

        if (currentUser == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(feedService.getClubFeed(clubId, currentUser.getId(), cursor, limit));
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : 1L; // Fallback to 1 for MVP testing
        boolean isLiked = feedService.toggleLike(postId, userId);
        return ResponseEntity.ok(Map.of("isLiked", isLiked));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(feedService.getComments(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentDto> addComment(
            @PathVariable Long postId,
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : 1L;
        CommentDto newComment = feedService.addComment(postId, userId, request.content());
        return ResponseEntity.ok(newComment);
    }
}