package ge.dola.talanti.user.dto;

public record PublicUserProfileDto(
        Long id,
        String username,
        String fullName,
        String role,
        String position,
        String preferredFoot,
        String bio,

        // Stats
        int followerCount,
        int followingCount,

        // Context for the React UI button
        boolean isFollowedByMe
) {}