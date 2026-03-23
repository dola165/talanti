package ge.dola.talanti.post;

import ge.dola.talanti.config.ResourceNotFoundException;
import ge.dola.talanti.feed.dto.CommentDto;
import ge.dola.talanti.jooq.tables.records.PostsRecord;
import ge.dola.talanti.post.dto.CreatePostDto;
import ge.dola.talanti.security.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        String normalizedContent = dto.content().trim();
        List<Long> normalizedMediaIds = dto.mediaIds() == null ? List.of() : dto.mediaIds().stream().distinct().toList();

        if (dto.clubId() != null && !postRepository.isUserAdminOfClub(currentUserId, dto.clubId())) {
            log.warn("SECURITY ALERT: User ID [{}] attempted to post for Club ID [{}] without permissions.",
                    safe(currentUserId), safe(dto.clubId()));
            throw new AccessDeniedException("You are not authorized to post on behalf of this organization.");
        }

        if (!postRepository.areAllMediaOwnedByUser(currentUserId, normalizedMediaIds)) {
            throw new IllegalArgumentException("All attached media must belong to the current user.");
        }

        PostsRecord savedPost = postRepository.createPost(currentUserId, normalizedContent, dto.clubId(), dto.isPublic());

        if (!normalizedMediaIds.isEmpty()) {
            postRepository.linkMediaToPost(savedPost.getId(), normalizedMediaIds);
        }

        return savedPost;
    }

    @Transactional
    @CacheEvict(cacheNames = "post-comments", key = "#postId")
    public void deletePost(Long postId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PostsRecord post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found."));

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
        if (!postRepository.canUserAccessPost(postId, currentUserId)) {
            throw new ResourceNotFoundException("Post not found.");
        }

        boolean isCurrentlyLiked = postRepository.isPostLikedByUser(postId, currentUserId);
        if (isCurrentlyLiked) {
            postRepository.unlikePost(postId, currentUserId);
            return false;
        }

        postRepository.likePost(postId, currentUserId);
        return true;
    }

    @Transactional
    @CacheEvict(cacheNames = "post-comments", key = "#postId")
    public CommentDto addComment(Long postId, String content) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!postRepository.canUserAccessPost(postId, currentUserId)) {
            throw new ResourceNotFoundException("Post not found.");
        }

        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isBlank()) {
            throw new IllegalArgumentException("Comment cannot be empty.");
        }

        return postRepository.addComment(postId, currentUserId, normalizedContent);
    }
}
