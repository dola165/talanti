package ge.dola.talanti.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostDto(
        @NotBlank(message = "Post content cannot be empty")
        @Size(max = 2000, message = "Post is too long")
        String content,

        // Optional: If the user is an admin of a club and posting on the club's behalf
        Long clubId,

        // Default to true for the MVP
        Boolean isPublic,

        // NEW: Accepts an array of media IDs uploaded previously
        List<Long> mediaIds
) {}