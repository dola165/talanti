package ge.dola.talanti.post;

import ge.dola.talanti.jooq.tables.records.PostsRecord;
import ge.dola.talanti.post.dto.CreatePostDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Import your custom utility ported from Grasskicks
import static ge.dola.talanti.util.LogSafe.safe;

@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public PostsRecord createPost(Long currentUserId, CreatePostDto dto) {
        // Sanitize log input to prevent CRLF log injection
        log.info("User ID [{}] is attempting to create a post.", safe(currentUserId));

        // Security Check: If posting as a club, verify they actually manage that club!
        if (dto.clubId() != null) {
            boolean isAdmin = postRepository.isUserAdminOfClub(currentUserId, dto.clubId());
            if (!isAdmin) {
                log.warn("SECURITY ALERT: User ID [{}] attempted to post on behalf of Club ID [{}] without permissions.",
                        safe(currentUserId), safe(dto.clubId()));
                throw new AccessDeniedException("You are not authorized to post on behalf of this organization.");
            }
        }

        PostsRecord savedPost = postRepository.createPost(
                currentUserId,
                dto.content(),
                dto.clubId(),
                dto.isPublic()
        );

        if (dto.mediaIds() != null && !dto.mediaIds().isEmpty()) {
            postRepository.linkMediaToPost(savedPost.getId(), dto.mediaIds());
            log.info("Successfully linked {} media items to Post ID [{}].",
                    safe(dto.mediaIds().size()), safe(savedPost.getId()));
        }

        log.info("Post ID [{}] created successfully by User ID [{}].",
                safe(savedPost.getId()), safe(currentUserId));
        return savedPost;
    }

    // IDOR Protection: Preventing users from deleting other people's posts
    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        PostsRecord post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));

        // IDOR CHECK: Is the person requesting the deletion the actual author?
        if (!post.getAuthorId().equals(currentUserId)) {
            log.warn("IDOR ALERT: User ID [{}] attempted to delete Post ID [{}] owned by User ID [{}].",
                    safe(currentUserId), safe(postId), safe(post.getAuthorId()));
            throw new AccessDeniedException("You can only delete your own posts.");
        }

        postRepository.delete(postId);
        log.info("Post ID [{}] successfully deleted by User ID [{}].", safe(postId), safe(currentUserId));
    }

    @Transactional
    public boolean toggleLike(Long postId, Long currentUserId) {
        if (!postRepository.postExists(postId)) {
            throw new IllegalArgumentException("Post not found.");
        }

        boolean isCurrentlyLiked = postRepository.isPostLikedByUser(postId, currentUserId);

        if (isCurrentlyLiked) {
            postRepository.unlikePost(postId, currentUserId);
            log.debug("User ID [{}] unliked Post ID [{}].", safe(currentUserId), safe(postId));
            return false;
        } else {
            postRepository.likePost(postId, currentUserId);
            log.debug("User ID [{}] liked Post ID [{}].", safe(currentUserId), safe(postId));
            return true;
        }
    }
}