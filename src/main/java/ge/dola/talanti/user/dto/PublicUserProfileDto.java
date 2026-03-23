package ge.dola.talanti.user.dto;

import ge.dola.talanti.user.UserType;

public record PublicUserProfileDto(
        Long id,
        String username,
        String fullName,
        UserType role,
        String position,
        String preferredFoot,
        String bio,
        String availabilityStatus,
        Integer heightCm,
        Integer weightKg,

        // Stats
        int followerCount,
        int followingCount,

        // Context for the React UI button
        boolean isFollowedByMe,
        String avatarUrl,
        String bannerUrl
) {}
