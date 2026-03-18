package ge.dola.talanti.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostDto(
        @NotBlank(message = "Post content cannot be empty")
        @Size(max = 2000, message = "Post is too long")
        String content,
        Long clubId,
        Boolean isPublic,
        List<Long> mediaIds
) {}