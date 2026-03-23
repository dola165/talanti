package ge.dola.talanti.feed;

import ge.dola.talanti.config.ResourceNotFoundException;
import ge.dola.talanti.feed.dto.CommentDto;
import ge.dola.talanti.feed.dto.FeedPostDto;
import ge.dola.talanti.feed.dto.FeedResponseDto;
import ge.dola.talanti.post.PostRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FeedService {

    private final FeedRepository feedRepository;
    private final PostRepository postRepository;

    public FeedService(FeedRepository feedRepository, PostRepository postRepository) {
        this.feedRepository = feedRepository;
        this.postRepository = postRepository;
    }

    public FeedResponseDto getFeed(Long currentUserId, Long cursor, int limit) {
        int actualLimit = Math.min(limit, 50);
        List<FeedPostDto> posts = feedRepository.getFeedForUser(currentUserId, cursor, actualLimit);
        Long nextCursor = (posts.size() == actualLimit && !posts.isEmpty()) ? posts.get(posts.size() - 1).id() : null;
        return new FeedResponseDto(posts, nextCursor);
    }

    public FeedResponseDto getClubFeed(Long clubId, Long currentUserId, Long cursor, int limit) {
        int actualLimit = Math.min(limit, 50);
        List<FeedPostDto> posts = feedRepository.getClubFeed(clubId, currentUserId, cursor, actualLimit);
        Long nextCursor = (posts.size() == actualLimit && !posts.isEmpty()) ? posts.get(posts.size() - 1).id() : null;
        return new FeedResponseDto(posts, nextCursor);
    }

    public FeedResponseDto getUserFeed(Long authorId, Long currentUserId, Long cursor, int limit) {
        int actualLimit = Math.min(limit, 50);
        List<FeedPostDto> posts = feedRepository.getUserFeed(authorId, currentUserId, cursor, actualLimit);
        Long nextCursor = (posts.size() == actualLimit && !posts.isEmpty()) ? posts.get(posts.size() - 1).id() : null;
        return new FeedResponseDto(posts, nextCursor);
    }

    @Cacheable(cacheNames = "post-comments", key = "#postId")
    public List<CommentDto> getComments(Long postId) {
        if (!postRepository.isPublicPost(postId)) {
            throw new ResourceNotFoundException("Post not found.");
        }
        return feedRepository.getCommentsForPost(postId);
    }
}
