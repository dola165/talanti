package ge.dola.talanti.feed.dto;

import java.util.List;

public record CreatePostRequest(
        String content,
        Long clubId, // Will be null if it's a personal post
        List<Long> mediaIds,
        List<Long> taggedUserIds
) {}