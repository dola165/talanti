package ge.dola.talanti.post;

import ge.dola.talanti.feed.FeedService;
import ge.dola.talanti.feed.dto.CommentDto;
import ge.dola.talanti.feed.dto.CommentRequest;
import ge.dola.talanti.feed.dto.FeedResponseDto;
import ge.dola.talanti.post.dto.CreatePostDto;
import ge.dola.talanti.security.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final FeedService feedService;

    /**
     * Safely resolves the current user ID for public endpoints.
     * Returns -1L if the user is unauthenticated so 'isLikedByMe' evaluates safely.
     */
    private Long resolveCurrentUserId() {
        return SecurityUtils.getCurrentUser().map(u -> u.getUserId()).orElse(-1L);
    }

    // --- READS (FeedService) ---

    @GetMapping("/feed")
    public ResponseEntity<FeedResponseDto> getGlobalFeed(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        // STRICT ENFORCEMENT: Calls getFeed() with the resolved context
        return ResponseEntity.ok(feedService.getFeed(resolveCurrentUserId(), cursor, limit));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<FeedResponseDto> getUserFeed(
            @PathVariable Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        // STRICT ENFORCEMENT: Pass resolved context
        return ResponseEntity.ok(feedService.getUserFeed(userId, resolveCurrentUserId(), cursor, limit));
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<FeedResponseDto> getClubFeed(
            @PathVariable Long clubId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        // STRICT ENFORCEMENT: Pass resolved context
        return ResponseEntity.ok(feedService.getClubFeed(clubId, resolveCurrentUserId(), cursor, limit));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(feedService.getComments(postId));
    }

    // --- WRITES (PostService) ---

    @PostMapping
    public ResponseEntity<?> createPost(@Valid @RequestBody CreatePostDto dto) {
        var savedPost = postService.createPost(dto);
        return ResponseEntity.ok(Map.of("message", "Post created", "postId", savedPost.getId()));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.ok(Map.of("message", "Post deleted successfully"));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long postId) {
        boolean isNowLiked = postService.toggleLike(postId);
        return ResponseEntity.ok(Map.of("isLiked", isNowLiked));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentDto> addComment(@PathVariable Long postId, @RequestBody CommentRequest request) {
        CommentDto newComment = postService.addComment(postId, request.content());
        return ResponseEntity.ok(newComment);
    }
}