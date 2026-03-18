package ge.dola.talanti.club.dto;

import java.util.List;

public record ClubProfileDto(
        Long id,
        String name,
        String description,
        String type,
        boolean isOfficial,

        int followerCount,
        int memberCount,

        // Context Flags for React UI conditional rendering
        boolean isFollowedByMe,
        boolean isMember,
        String myRole, // "OWNER", "CLUB_ADMIN", "COACH", or null if not a member

        String addressText,
        String logoUrl,
        String bannerUrl,

        // Directly embed the opportunities so the UI renders instantly
        List<ClubOpportunityDto> opportunities
) {}