package ge.dola.talanti.feed;

import ge.dola.talanti.feed.dto.CreatePostRequest;
import ge.dola.talanti.feed.dto.FeedPostDto;
import ge.dola.talanti.feed.dto.FeedResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedService {

    private final FeedRepository feedRepository;

    public FeedService(FeedRepository feedRepository) {
        this.feedRepository = feedRepository;
    }

    public FeedResponseDto getFeed(Long currentUserId, Long cursor, int limit) {
        // 1. Safety check: Cap the limit so the frontend can't request 10,000 posts at once
        int actualLimit = Math.min(limit, 50);

        // 2. Fetch the posts from our massive JOOQ query
        List<FeedPostDto> posts = feedRepository.getFeedForUser(currentUserId, cursor, actualLimit);

        // 3. Determine the next cursor
        Long nextCursor = null;
        // If the size of the returned list matches our limit, there are probably more posts in the database.
        if (posts.size() == actualLimit && !posts.isEmpty()) {
            nextCursor = posts.get(posts.size() - 1).id();
        }

        // 4. Return the wrapped response
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

        Long nextCursor = null;
        if (posts.size() == actualLimit && !posts.isEmpty()) {
            nextCursor = posts.get(posts.size() - 1).id();
        }

        return new FeedResponseDto(posts, nextCursor);
    }

    public boolean toggleLike(Long postId, Long userId) {
        return feedRepository.toggleLike(postId, userId);
    }

    public java.util.List<ge.dola.talanti.feed.dto.CommentDto> getComments(Long postId) {
        return feedRepository.getCommentsForPost(postId);
    }

    public ge.dola.talanti.feed.dto.CommentDto addComment(Long postId, Long userId, String content) {
        return feedRepository.addComment(postId, userId, content);
    }

    public void createPost(Long authorId, CreatePostRequest request) {
        feedRepository.createPost(authorId, request);
    }

}