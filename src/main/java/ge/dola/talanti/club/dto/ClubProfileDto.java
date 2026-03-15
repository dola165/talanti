package ge.dola.talanti.club.dto;

public record ClubProfileDto(
        Long id,
        String name,
        String description,
        String type,
        boolean isOfficial,

        int followerCount,
        int memberCount,

        boolean isFollowedByMe,
        boolean isMember,
        String addressText,


        String logoUrl,
        String bannerUrl
) {}