package ge.dola.talanti.feed.dto;

import java.time.LocalDateTime;

public record CommentDto(
        Long id,
        String authorName,
        String authorAvatarUrl,
        String content,
        LocalDateTime createdAt
) {}
