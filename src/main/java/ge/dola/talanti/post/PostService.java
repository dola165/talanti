package ge.dola.talanti.post;

import ge.dola.talanti.jooq.tables.records.PostsRecord;
import ge.dola.talanti.post.dto.CreatePostDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public PostsRecord createPost(Long currentUserId, CreatePostDto dto) {
        // You could add validation here: "Is this user actually a member of this clubId?"
        // For the 3-day MVP, we will keep it open and simple.

        return postRepository.createPost(
                currentUserId,
                dto.content(),
                dto.clubId(),
                dto.isPublic()
        );
    }
    /**
     * Toggles the like status of a post.
     * Returns true if the post is now liked, false if unliked.
     */
    @Transactional
    public boolean toggleLike(Long postId, Long currentUserId) {
        // 1. Verify the post exists
        if (!postRepository.postExists(postId)) {
            throw new RuntimeException("Post not found with id: " + postId);
        }

        // 2. Check current status
        boolean isCurrentlyLiked = postRepository.isPostLikedByUser(postId, currentUserId);

        // 3. Toggle
        if (isCurrentlyLiked) {
            postRepository.unlikePost(postId, currentUserId);
            return false;
        } else {
            postRepository.likePost(postId, currentUserId);
            return true;
        }
    }
}