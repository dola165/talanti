package ge.dola.talanti.feed.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FeedPostDto(
        Long id,
        String content,
        LocalDateTime createdAt,

        // Author details
        Long authorId,
        String authorName,

        // Club details (will be null if it's a personal post, not a club post)
        Long clubId,
        String clubName,

        // Interaction Stats
        int likeCount,
        int commentCount,
        boolean isLikedByMe, // Crucial for the frontend to render the heart icon red or outline

        // Media (Just returning a list of URL strings for the MVP)
        List<String> mediaUrls
) {}