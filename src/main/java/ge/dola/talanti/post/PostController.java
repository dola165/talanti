package ge.dola.talanti.post;

import ge.dola.talanti.feed.FeedService;
import ge.dola.talanti.feed.dto.CommentDto;
import ge.dola.talanti.feed.dto.CommentRequest;
import ge.dola.talanti.feed.dto.FeedResponseDto;
import ge.dola.talanti.post.dto.CreatePostDto;
import ge.dola.talanti.security.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Validated
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
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(feedService.getFeed(resolveCurrentUserId(), cursor, limit));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<FeedResponseDto> getUserFeed(
            @PathVariable @Positive Long userId,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(feedService.getUserFeed(userId, resolveCurrentUserId(), cursor, limit));
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<FeedResponseDto> getClubFeed(
            @PathVariable @Positive Long clubId,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(feedService.getClubFeed(clubId, resolveCurrentUserId(), cursor, limit));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable @Positive Long postId) {
        return ResponseEntity.ok(feedService.getComments(postId));
    }

    // --- WRITES (PostService) ---

    @PostMapping
    public ResponseEntity<?> createPost(@Valid @RequestBody CreatePostDto dto) {
        var savedPost = postService.createPost(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Post created", "postId", savedPost.getId()));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable @Positive Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.ok(Map.of("message", "Post deleted successfully"));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable @Positive Long postId) {
        boolean isNowLiked = postService.toggleLike(postId);
        return ResponseEntity.ok(Map.of("isLiked", isNowLiked));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentDto> addComment(@PathVariable @Positive Long postId, @Valid @RequestBody CommentRequest request) {
        CommentDto newComment = postService.addComment(postId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(newComment);
    }
}
