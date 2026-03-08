package ge.dola.talanti.user.dto;

import java.util.List;

public record PublicUserProfileDto(
        Long id,
        String username,
        String fullName,
        String position,
        String preferredFoot,
        String bio,

        // --- NEW SCOUTING METRICS ---
        String availabilityStatus, // e.g., "FREE_AGENT", "IN_CLUB", "OPEN_TO_OFFERS"
        Integer heightCm,
        Integer weightKg,

        // Stats
        int followerCount,
        int followingCount,

        // Context
        boolean isFollowedByMe,

        // --- NEW CAREER DATA ---
        List<CareerHistoryDto> careerHistory
) {}