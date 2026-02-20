package ge.dola.talanti.club.dto;

public record ClubProfileDto(
        Long id,
        String name,
        String description,
        String type,
        boolean isOfficial,

        // Stats
        int followerCount,
        int memberCount,

        // Logged-in User's Context
        boolean isFollowedByMe,
        boolean isMember
) {}