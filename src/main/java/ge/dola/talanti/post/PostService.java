package ge.dola.talanti.post;

import ge.dola.talanti.feed.dto.CommentDto;
import ge.dola.talanti.jooq.tables.records.PostsRecord;
import ge.dola.talanti.post.dto.CreatePostDto;
import ge.dola.talanti.security.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static ge.dola.talanti.security.util.LogSafe.safe;

@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public PostsRecord createPost(CreatePostDto dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User ID [{}] attempting to create a post.", safe(currentUserId));

        if (dto.clubId() != null) {
            if (!postRepository.isUserAdminOfClub(currentUserId, dto.clubId())) {
                log.warn("SECURITY ALERT: User ID [{}] attempted to post for Club ID [{}] without permissions.",
                        safe(currentUserId), safe(dto.clubId()));
                throw new AccessDeniedException("You are not authorized to post on behalf of this organization.");
            }
        }

        PostsRecord savedPost = postRepository.createPost(
                currentUserId, dto.content(), dto.clubId(), dto.isPublic()
        );

        if (dto.mediaIds() != null && !dto.mediaIds().isEmpty()) {
            postRepository.linkMediaToPost(savedPost.getId(), dto.mediaIds());
        }

        return savedPost;
    }

    @Transactional
    public void deletePost(Long postId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PostsRecord post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));

        // STRICT ENFORCEMENT: Club Posts vs Personal Posts
        if (post.getClubId() != null) {
            if (!postRepository.isUserAdminOfClub(currentUserId, post.getClubId())) {
                log.warn("IDOR ALERT: User ID [{}] attempted to delete Club Post ID [{}].", safe(currentUserId), safe(postId));
                throw new AccessDeniedException("You must be a club admin to delete this post.");
            }
        } else if (!post.getAuthorId().equals(currentUserId)) {
            log.warn("IDOR ALERT: User ID [{}] attempted to delete Personal Post ID [{}].", safe(currentUserId), safe(postId));
            throw new AccessDeniedException("You can only delete your own posts.");
        }

        postRepository.delete(postId);
        log.info("Post ID [{}] deleted by User ID [{}].", safe(postId), safe(currentUserId));
    }

    @Transactional
    public boolean toggleLike(Long postId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!postRepository.postExists(postId)) {
            throw new IllegalArgumentException("Post not found.");
        }

        boolean isCurrentlyLiked = postRepository.isPostLikedByUser(postId, currentUserId);
        if (isCurrentlyLiked) {
            postRepository.unlikePost(postId, currentUserId);
            return false;
        } else {
            postRepository.likePost(postId, currentUserId);
            return true;
        }
    }


    @Transactional
    public CommentDto addComment(Long postId, String content) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!postRepository.postExists(postId)) {
            throw new IllegalArgumentException("Post not found.");
        }
        return postRepository.addComment(postId, currentUserId, content);
    }
}