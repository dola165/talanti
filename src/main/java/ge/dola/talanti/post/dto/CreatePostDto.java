package ge.dola.talanti.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostDto(
        @NotBlank(message = "Post content cannot be empty")
        @Size(max = 2000, message = "Post is too long")
        String content,
        Long clubId,
        Boolean isPublic,
        @Size(max = 10, message = "A post can include at most 10 media attachments")
        List<@NotNull(message = "Media ID cannot be null") @Positive(message = "Media ID must be positive") Long> mediaIds
) {}
