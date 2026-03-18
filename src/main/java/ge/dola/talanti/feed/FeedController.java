//package ge.dola.talanti.feed;
//
//import ge.dola.talanti.feed.dto.CommentDto;
//import ge.dola.talanti.feed.dto.CommentRequest;
//import ge.dola.talanti.feed.dto.CreatePostRequest;
//import ge.dola.talanti.feed.dto.FeedResponseDto;
//import ge.dola.talanti.security.CustomUserDetails;
//import ge.dola.talanti.security.util.SecurityUtils;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/feed")
//@RequiredArgsConstructor
//public class FeedController {
//
//    private final FeedService feedService;
//
//
//
//    @GetMapping("/feed") // Maps to /api/posts/feed
//    public ResponseEntity<FeedResponseDto> getFeed(
//            @RequestParam(required = false) Long cursor,
//            @RequestParam(defaultValue = "20") int limit) {
//        Long userId = SecurityUtils.getCurrentUser().map(CustomUserDetails::getUserId).orElse(null);
//        return ResponseEntity.ok(feedService.getFeed(userId, cursor, limit));
//    }
//
//    @PostMapping
//    public ResponseEntity<?> createPost(@RequestBody CreatePostRequest request) {
//        feedService.createPost(SecurityUtils.getCurrentUserId(), request);
//        return ResponseEntity.ok(Map.of("message", "Post created successfully"));
//    }
//
//    @PostMapping("/{postId}/like")
//    public ResponseEntity<?> toggleLike(@PathVariable Long postId) {
//        boolean isLiked = feedService.toggleLike(postId, SecurityUtils.getCurrentUserId());
//        return ResponseEntity.ok(Map.of("isLiked", isLiked));
//    }
//
//    @PostMapping("/{postId}/comments")
//    public ResponseEntity<CommentDto> addComment(@PathVariable Long postId, @RequestBody CommentRequest request) {
//        CommentDto newComment = feedService.addComment(postId, SecurityUtils.getCurrentUserId(), request.content());
//        return ResponseEntity.ok(newComment);
//    }
//
//    /**
//     * React will call: GET /api/feed?limit=20
//     * To get the next page: GET /api/feed?cursor=150&limit=20
//     */
//    @GetMapping
//    public ResponseEntity<FeedResponseDto> getFeed(
//            @AuthenticationPrincipal CustomUserDetails currentUser,
//            @RequestParam(required = false) Long cursor,
//            @RequestParam(defaultValue = "20") int limit) {
//
//        // Safety fallback (though our SecurityConfig should block unauthenticated requests before they get here)
//        if (currentUser == null) {
//            return ResponseEntity.status(401).build();
//        }
//
//        // Pass the request to the service layer
//        FeedResponseDto response = feedService.getFeed(currentUser.getId(), cursor, limit);
//
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/club/{clubId}")
//    public ResponseEntity<FeedResponseDto> getClubFeed(
//            @PathVariable Long clubId,
//            @AuthenticationPrincipal CustomUserDetails currentUser,
//            @RequestParam(required = false) Long cursor,
//            @RequestParam(defaultValue = "20") int limit) {
//
//        if (currentUser == null) return ResponseEntity.status(401).build();
//        return ResponseEntity.ok(feedService.getClubFeed(clubId, currentUser.getId(), cursor, limit));
//    }
//
//    @GetMapping("/user/{authorId}")
//    public ResponseEntity<FeedResponseDto> getUserFeed(
//            @PathVariable Long authorId,
//            @AuthenticationPrincipal CustomUserDetails currentUser,
//            @RequestParam(required = false) Long cursor,
//            @RequestParam(defaultValue = "20") int limit) {
//
//        Long userId = currentUser != null ? currentUser.getId() : 1L; // Fallback to 1 for MVP testing
//        return ResponseEntity.ok(feedService.getUserFeed(authorId, userId, cursor, limit));
//    }
//
//    @PostMapping("/posts/{postId}/like")
//    public ResponseEntity<?> toggleLike(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails currentUser) {
//        Long userId = currentUser != null ? currentUser.getId() : 1L; // Fallback to 1 for MVP testing
//        boolean isLiked = feedService.toggleLike(postId, userId);
//        return ResponseEntity.ok(Map.of("isLiked", isLiked));
//    }
//
//    @GetMapping("/posts/{postId}/comments")
//    public ResponseEntity<List<CommentDto>> getComments(@PathVariable Long postId) {
//        return ResponseEntity.ok(feedService.getComments(postId));
//    }
//
//    @PostMapping("/posts/{postId}/comments")
//    public ResponseEntity<CommentDto> addComment(
//            @PathVariable Long postId,
//            @RequestBody CommentRequest request,
//            @AuthenticationPrincipal CustomUserDetails currentUser) {
//        Long userId = currentUser != null ? currentUser.getId() : 1L;
//        CommentDto newComment = feedService.addComment(postId, userId, request.content());
//        return ResponseEntity.ok(newComment);
//    }
//
//
//    @PostMapping("/posts")
//    public ResponseEntity<?> createPost(
//            @RequestBody CreatePostRequest request,
//            @AuthenticationPrincipal CustomUserDetails currentUser) {
//
//        Long userId = currentUser != null ? currentUser.getId() : 1L; // Fallback for MVP
//        feedService.createPost(userId, request);
//        return ResponseEntity.ok(Map.of("message", "Post created successfully"));
//    }
//}