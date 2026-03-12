package ge.dola.talanti.post;

import ge.dola.talanti.post.dto.CreatePostDto;
import ge.dola.talanti.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<?> createPost(
            @Valid @RequestBody CreatePostDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            var savedPost = postService.createPost(currentUser.getId(), dto);

            // Return success with the new post ID.
            // The React dev can either append this locally or just re-fetch the feed.
            return ResponseEntity.ok(Map.of(
                    "message", "Post created successfully",
                    "postId", savedPost.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * React will call: POST /api/posts/{id}/like
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            boolean isNowLiked = postService.toggleLike(id, currentUser.getId());

            // Returns { "isLiked": true } so the frontend can immediately turn the heart red
            return ResponseEntity.ok(Map.of("isLiked", isNowLiked));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/posts/{id}/comments
    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        // We will build the proper JOOQ repository fetch for this next,
        // but this stops the 403 error immediately!
        return ResponseEntity.ok(List.of());
    }

    // POST /api/posts/{id}/comments
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal CustomUserDetails user) {

        Long userId = user != null ? user.getId() : 1L; // MVP Bypass
        String content = payload.get("content");

        // Return a mock success so React updates locally
        return ResponseEntity.ok(Map.of("commentId", System.currentTimeMillis()));
    }
}