package ge.dola.talanti.dto;

import java.time.LocalDateTime;

public class UserDTOs {

    /**
     * LIGHTWEIGHT: For lists, chat bubbles, and sidebars.
     */
    public record UserSummary(
            Long id,
            String username,
            String fullName// From user_profiles
    ) {}

    /**
     * HEAVYWEIGHT: For the "My Profile" page.
     */
    public record UserProfileDetail(
            Long userId,
            String username,
            String email,
            String fullName,
            String bio,
            String position,       // e.g., "Striker"
            String preferredFoot,  // e.g., "Right"
            Integer age,
            LocalDateTime joinedAt
    ) {}
}