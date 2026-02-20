package ge.dola.talanti.feed.dto;

import java.util.List;

public record FeedResponseDto(
        List<FeedPostDto> posts,
        Long nextCursor // If null, the frontend knows they hit the end of the feed
) {}